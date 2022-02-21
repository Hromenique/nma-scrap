package br.com.hrom.nmascrap

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.imageio.ImageIO

class NMAScraper(
    private val userName: String,
    private val password: String,
    private val lessonUrl: URL,
    private val preferredResolution: Resolution,
    private val destinationFolder: File,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) {

    fun doScrap() {
        println("Start NMA scrap")

        val session = doLogin()
        val lessonPage = getLessonPage(session, lessonUrl)

        val courseName = getCourseName(lessonPage)
        val lessonName = getLessonName(lessonPage)
        val lessonDetails = getLessonDetails(lessonPage)
        val pdfReferences = getPdfReferences(session, lessonPage)
        val imageReferences = getImageReferences(lessonPage)
        val videoReferences = getVideoReferences(lessonPage)

        val courseFolder = createFolderIfNotExists(File(destinationFolder, courseName))
        val lessonFolder = createFolderIfNotExists(File(courseFolder, lessonName))

        writeLessonDetails(lessonDetails, destFolder = lessonFolder)
        downloadPdfs(pdfReferences, destFolder = lessonFolder)
        downloadImages(imageReferences, destFolder = lessonFolder)
        downloadVideos(videoReferences, preferredResolution, destFolder = lessonFolder)

        println("Finished")
    }

    private fun doLogin(): Connection {
        val session = Jsoup.newSession()
        session
            .header(ORIGIN_HEADER, ORIGIN_HTTP_ADDRESS)
            .url("https://www.nma.art/welcome-back/")
            .data("log", userName)
            .data("pwd", password)
            .data("rememberme", "forever")
            .data("wp-submit", "Log+In")
            .data("redirect_to", "https://www.nma.art")
            //        .data("testcookie", "1")
            .post()

        return session
    }

    private fun getLessonPage(session: Connection, lessonUrl: URL) = session.url(lessonUrl).get()

    private fun getCourseName(lessonPage: Document): String {
        return lessonPage
            .body()
            .selectFirst("#grayRectangle > div.col-full > div.nma_bread > div.bread-left > div > a")
            ?.let { a ->
                val link = a.attr("href")
                val (name) = ".+/(?<name>.+)/".toRegex().find(link)?.destructured
                    ?: throw IllegalStateException("Course name not found on page $lessonUrl")
                name.normalizeToFolderName()
            }
            ?: throw IllegalStateException("Course name not found on page $lessonUrl")
    }

    private fun getLessonName(lessonPage: Document): String {
        return lessonPage
            .body()
            .selectFirst("#grayRectangle > div.col-full > div.nma_bread > div.bread-left > div > h1")
            ?.text()
            ?.normalizeToFolderName()
            ?: throw IllegalStateException("Lesson name not found on page $lessonUrl")
    }

    private fun getLessonDetails(lessonPage: Document): String {
        return lessonPage
            .body()
            .selectFirst("#details .lessondesc")
            ?.getElementsByTag("p")
            ?.joinToString("\n") { p -> p.text() }
            ?: throw IllegalStateException("Lesson Details not found on page $lessonUrl")
    }


    private fun getImageReferences(lessonPage: Document): List<ImageReference> {
        return lessonPage
            .body()
            .select("#references .reference-container a")
            .map { a ->
                val link = a.attr("href")
                ImageReference(URL(link))
            }
    }

    private fun getPdfViewerReferences(lessonPage: Document): List<PdfViewerReference> {
        return lessonPage
            .body()
            .select(".viewer-pdf-link a")
            .map { a -> PdfViewerReference(URL(a.attr("href"))) }
    }

    private fun getPdfReferences(session: Connection, lessonPage: Document): List<PdfReference> {
        return getPdfViewerReferences(lessonPage)
            .map { pdfViewerReference -> getPdfViewerPage(session, pdfViewerReference) }
            .map { page -> getPdfUrl(page) }
            .map { url -> PdfReference(url) }
    }

    private fun getVideoReferences(lessonPage: Document): List<VideoReference> {
        val rawJsPlayListRegex = "playlistSource\\s*=\\s*(.+);".toRegex()
        val playlistAsRawJsArray = lessonPage
            .body()
            .getElementsByTag("script")
            .filter { it.childNodeSize() == 1 } // only script tag with on child (CDATA)
            .find {
                val outerHtml = it.childNode(0).outerHtml()
                outerHtml.contains("var isPlaying = false;")
                        && outerHtml.contains("var playlist;")
                        && outerHtml.contains("var listIDs")
            }
            ?.outerHtml()
            ?.let { rawHtml ->
                rawJsPlayListRegex.find(rawHtml)?.destructured?.component1()
                    ?: throw IllegalStateException("PlayList not found on page ${lessonPage.baseUri()}")
            }
            ?: throw IllegalStateException("PlayList not found on page ${lessonPage.baseUri()}")


        return objectMapper.readJSObject(playlistAsRawJsArray, object : TypeReference<List<VideoReference>>() {})
    }

    private fun createFolderIfNotExists(folder: File): File {
        if (!folder.exists()) {
            folder.mkdir()
        }
        return folder
    }

    private fun writeLessonDetails(lessonDetails: String, destFolder: File) {
        File(destFolder, LESSON_DETAILS_FILE_NAME).writeText(lessonDetails, Charsets.UTF_8)
    }

    private fun getPdfViewerPage(session: Connection, pdfViewerReference: PdfViewerReference): Document {
        return session
            .header(ORIGIN_HEADER, ORIGIN_HTTP_ADDRESS)
            .url(pdfViewerReference.url)
            .get()
    }

    private fun getPdfUrl(pdfViewerPage: Document): URL {
        val pdfUrlRegex = "let\\s+attachment_url\\s*=\\s*\"(?<pdfUrl>.+)\";".toRegex()
        return pdfViewerPage
            .head()
            .getElementsByTag("script")
            .filter { it.childNodeSize() == 1 } // only script tag with one child (CDATA)
            .find {
                val outerHtml = it.childNode(0).outerHtml()
                outerHtml.contains("let attachment_url") && outerHtml.contains("let HOSTED_VIEWER_ORIGINS")
            }
            ?.outerHtml() // script tag content
            ?.let {
                pdfUrlRegex.find(it)?.destructured?.component1()
                    ?: throw IllegalStateException("PDF url not found on page ${pdfViewerPage.baseUri()}")
            }
            ?.let { URL(it) }
            ?: throw IllegalStateException("PDF url not found on page ${pdfViewerPage.baseUri()}")
    }

    private fun downloadPdfs(pdfReferences: List<PdfReference>, destFolder: File) {
        println("== Start download pdfs ==")
        var errorCount = 0

        pdfReferences.forEach { pdf ->
            try {
                pdf.url.openStreamToResource().use { input ->
                    Files.copy(input, File(destFolder, pdf.name).toPath(), REPLACE_EXISTING)
                    println("${pdf.url} download done")
                }
            } catch (ex: Exception) {
                errPrintln("${pdf.url} download failed: ${ex.message}")
                errorCount++
            }
        }

        when {
            pdfReferences.isEmpty() -> println("It was not found pdfs to download")
            errorCount == pdfReferences.size -> println("All pdf download failed")
            errorCount > 0 -> println("Pdf download finished with errors")
            else -> println("All pdfs downloaded")
        }
    }

    private fun downloadImages(imageReferences: List<ImageReference>, destFolder: File) {
        println("== Start download images ==")
        var errorCount = 0

        imageReferences.forEach { image ->
            try {
                image.url.openStreamToResource().use { input ->
                    Files.copy(input, File(destFolder, image.name).toPath(), REPLACE_EXISTING)
                    println("${image.url} download done")
                }
            } catch (ex: Exception) {
                errPrintln("${image.url} download failed: ${ex.message}")
                errorCount++
            }
        }

//        imageReferences.forEach { image ->
//            try {
//                val imageFile = ImageIO.read(image.url)
//                ImageIO.write(imageFile, image.format, File(destFolder, image.name.normalizeToFileName()))
//                println("${image.url} download done")
//            } catch (ex: Exception) {
//                errPrintln("${image.url} download failed: ${ex.message}")
//                errorCount++
//            }
//        }

        when {
            imageReferences.isEmpty() -> println("It was not found images to download")
            errorCount == imageReferences.size -> println("All images download failed")
            errorCount > 0 -> println("Image download finished with errors")
            else -> println("All images downloaded")
        }
    }

    private fun downloadVideos(
        videoReferences: List<VideoReference>,
        preferredResolution: Resolution,
        destFolder: File,
    ) {
        println("== Start download videos ==")
        var errorCount = 0

        videoReferences.forEachIndexed { i, video ->
            val videoSource = video.tryGetSourceWithResolution(preferredResolution)
            val maybeTrack = video.tracks.firstOrNull()
            val name = "${i + 1} ${video.title.normalizeToFileName()}"

            try {
                videoSource.url.openStreamToResource().use { input ->
                    Files.copy(input,
                        File(destFolder, "$name.${videoSource.extension ?: "mp4"}").toPath(),
                        REPLACE_EXISTING)
                    println("${videoSource.url} download done")
                }
            } catch (ex: Exception) {
                errorCount++
                errPrintln("${videoSource.url} download failed: ${ex.message}")
            }

            maybeTrack?.let { track ->
                try {
                    track.url.openStreamToResource().use { input ->
                        Files.copy(input,
                            File(destFolder, "$name.${track.extension ?: "vtt"}").toPath(),
                            REPLACE_EXISTING)
                        println("${track.url} download done")
                    }
                } catch (ex: Exception) {
                    errPrintln("${track.url} download failed: ${ex.message}")
                }
            }
        }

        when {
            videoReferences.isEmpty() -> println("It was not found videos to download")
            errorCount == videoReferences.size -> println("All videos download failed")
            errorCount > 0 -> println("Video download finished with errors")
            else -> println("All videos downloaded")
        }
    }

    private fun URL.openStreamToResource(): InputStream {
        val urlConnection = this.openConnection() as HttpURLConnection
        urlConnection.setRequestProperty(ORIGIN_HEADER, ORIGIN_HTTP_ADDRESS)
        urlConnection.setRequestProperty(REFERER_HEADER, lessonUrl.toString())
        urlConnection.doOutput = true
        urlConnection.doInput = true
        return urlConnection.inputStream
    }

    companion object {
        private const val LESSON_DETAILS_FILE_NAME = "lesson details.txt"
        private const val ORIGIN_HTTP_ADDRESS = "https://www.nma.art"

        private const val REFERER_HEADER = "Referer"
        private const val ORIGIN_HEADER = "Origin"
    }
}