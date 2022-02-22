package br.com.hrom.nmascrap

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class NMAScraper(
    private val userName: String,
    private val password: String,
    private val courseUrl: URL,
    private val preferredResolution: Resolution,
    private val destinationFolder: File,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) {
    private val log = LoggerFactory.getLogger("Scraper")

    fun doScrap() {
        log.info("========== Start NMA scrap ==========")
        log.info("Indexing resources...")

        val session = doLogin()

        val coursePage = getCoursePage(session, courseUrl)
        val courseName = getCourseName(coursePage)
        val courseDetails = getCourseDetails(coursePage)
        val lessons = getLessons(session, coursePage)

        val courseFolder = createFolderIfNotExists(File(destinationFolder, courseName))
        writeCourseDetails(courseDetails, destFolder = courseFolder)

        lessons.forEach { lesson ->
            log.info("\n===== ${lesson.name} =====")
            val lessonFolder = createFolderIfNotExists(File(courseFolder, lesson.name))
            writeLessonDetails(lesson.details, destFolder = lessonFolder)
            downloadPdfs(lesson.pdfReferences, destFolder = lessonFolder)
            downloadImages(lesson.imageReferences, destFolder = lessonFolder)
            downloadVideos(lesson.videoReferences, preferredResolution, destFolder = lessonFolder)
        }

        log.info("========== Finished ==========")
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

    private fun getCoursePage(session: Connection, courseUrl: URL) = session.url(courseUrl).get()

    private fun getCourseDetails(coursePage: Document): String {
        return coursePage
            .body()
            .selectFirst("#inner-wrapper > div.lpb-container > div > div > div > p.learning-path-excerpt")
            ?.text()
            ?: throw IllegalStateException("Course Details not found on page ${coursePage.baseUri()}")
    }

    private fun getLessons(session: Connection, coursePage: Document): List<Lesson> {
        return getLessonReferences(coursePage).map { lesson ->
            val lessonPage = getLessonPage(session, lesson.url)
            val lessonName = lesson.name
            val lessonDetails = getLessonDetails(lessonPage)
            val pdfReferences = getPdfReferences(session, lessonPage)
            val imageReferences = getImageReferences(lessonPage)
            val videoReferences = getVideoReferences(lessonPage)
            Lesson(lessonName, lessonDetails, pdfReferences, imageReferences, videoReferences)
        }
    }

    private fun getLessonReferences(coursePage: Document): List<LessonReference> {
        return coursePage
            .body()
            .select("#post-items div.lesson-preview-content > h2.title > a")
            .mapIndexed { i, a ->
                val lessonIndex = i + 1
                val title = a.attr("title")
                    .normalizeToFolderName()
                    .let { title ->
                        if (title.startsWith("$lessonIndex")) title
                        else "$lessonIndex $title"
                    }
                val link = a.attr("href")
                LessonReference(title, URL(link))
            }
    }

    private fun getLessonPage(session: Connection, lessonUrl: URL) = session.url(lessonUrl).get()

    private fun getCourseName(coursePage: Document): String {
        return coursePage
            .body()
            .selectFirst("#inner-wrapper > div.lpb-container > div > div > div > h1")
            ?.let { h1 ->
                h1.text().normalizeToFolderName().takeIf { it.isNotBlank() }
            }
            ?: throw IllegalStateException("Course name not found on page ${coursePage.baseUri()}")
    }

    private fun getLessonDetails(lessonPage: Document): String {
        return lessonPage
            .body()
            .selectFirst("#details .lessondesc")
            ?.getElementsByTag("p")
            ?.joinToString("\n") { p -> p.text() }
            ?: throw IllegalStateException("Lesson Details not found on page ${lessonPage.baseUri()}")
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

    private fun writeCourseDetails(courseDetails: String, destFolder: File) {
        File(destFolder, COURSE_DETAILS_FILE_NAME).writeText(courseDetails, Charsets.UTF_8)
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
        log.info("== Start download pdfs ==")
        var errorCount = 0

        pdfReferences.forEach { pdf ->
            try {
                pdf.url.openStreamToResource().use { input ->
                    Files.copy(input, File(destFolder, pdf.name).toPath(), REPLACE_EXISTING)
                    log.info("${pdf.url} download done")
                }
            } catch (ex: Exception) {
                log.error("${pdf.url} download failed: ${ex.message}")
                errorCount++
            }
        }

        when {
            pdfReferences.isEmpty() -> log.info("It was not found pdfs to download")
            errorCount == pdfReferences.size -> log.info("All pdf download failed")
            errorCount > 0 -> log.info("Pdf download finished with errors")
            else -> log.info("All pdfs downloaded")
        }
    }

    private fun downloadImages(imageReferences: List<ImageReference>, destFolder: File) {
        log.info("== Start download images ==")
        var errorCount = 0

        imageReferences.forEach { image ->
            try {
                image.url.openStreamToResource().use { input ->
                    Files.copy(input, File(destFolder, image.name).toPath(), REPLACE_EXISTING)
                    log.info("${image.url} download done")
                }
            } catch (ex: Exception) {
                log.error("${image.url} download failed: ${ex.message}")
                errorCount++
            }
        }

        when {
            imageReferences.isEmpty() -> log.info("It was not found images to download")
            errorCount == imageReferences.size -> log.info("All images download failed")
            errorCount > 0 -> log.info("Image download finished with errors")
            else -> log.info("All images downloaded")
        }
    }

    private fun downloadVideos(
        videoReferences: List<VideoReference>,
        preferredResolution: Resolution,
        destFolder: File,
    ) {
        log.info("== Start download videos ==")
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
                    log.info("${videoSource.url} download done")
                }
            } catch (ex: Exception) {
                errorCount++
                log.error("${videoSource.url} download failed: ${ex.message}")
            }

            maybeTrack?.let { track ->
                try {
                    track.url.openStreamToResource().use { input ->
                        Files.copy(input,
                            File(destFolder, "$name.${track.extension ?: "vtt"}").toPath(),
                            REPLACE_EXISTING)
                        log.info("${track.url} download done")
                    }
                } catch (ex: Exception) {
                    log.info("${track.url} download failed: ${ex.message}")
                }
            }
        }

        when {
            videoReferences.isEmpty() -> log.info("It was not found videos to download")
            errorCount == videoReferences.size -> log.info("All videos download failed")
            errorCount > 0 -> log.info("Video download finished with errors")
            else -> log.info("All videos downloaded")
        }
    }

    private fun URL.openStreamToResource(): InputStream {
        val urlConnection = this.openConnection() as HttpURLConnection
        urlConnection.setRequestProperty(ORIGIN_HEADER, ORIGIN_HTTP_ADDRESS)
        urlConnection.setRequestProperty(REFERER_HEADER, courseUrl.toString())
        urlConnection.doOutput = true
        urlConnection.doInput = true
        return urlConnection.inputStream
    }

    companion object {
        private const val LESSON_DETAILS_FILE_NAME = "lesson details.txt"
        private const val COURSE_DETAILS_FILE_NAME = "course details.txt"

        private const val ORIGIN_HTTP_ADDRESS = "https://www.nma.art"

        private const val REFERER_HEADER = "Referer"
        private const val ORIGIN_HEADER = "Origin"
    }
}