package br.com.hrom.nmascrap.m3u8

import br.com.hrom.nmascrap.commons.openStreamToResource
import br.com.hrom.nmascrap.commons.retryOnError
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.jvm.Throws

class M3u8PlayListReader(private val masterM3u8Entry: MasterM3u8Entry) {

    @Throws(IOException::class)
    fun readSubtitlesUrl(): URL? {
        if (masterM3u8Entry.subtitleFileURL == null) return null

        val baseSubtitleUri = masterM3u8Entry.subtitleFileURL.toURI()
        val subtitleSegmentFileAsString =
            retryOnError { masterM3u8Entry.subtitleFileURL.openStreamToResource().use { String(it.readAllBytes()) } }
        return extractSubtitleSegment(subtitleSegmentFileAsString)?.let { baseSubtitleUri.resolve(it).toURL() }
    }

    @Throws(IOException::class)
    fun subtitleInputStream(): InputStream? = retryOnError {
        this.readSubtitlesUrl()?.openStreamToResource()
    }

    @Throws(IOException::class)
    fun readAudioStreamUrls(): List<URL> {
        if (masterM3u8Entry.audioFileURL == null) return emptyList()

        val baseAudioUri = masterM3u8Entry.audioFileURL.toURI()
        val tsSegmentFileAsString = retryOnError {
            masterM3u8Entry.audioFileURL.openStreamToResource().use { String(it.readAllBytes()) }
        }

        return extractAllTsFilesSegments(tsSegmentFileAsString).map { baseAudioUri.resolve(it).toURL() }
    }

    @Throws(IOException::class)
    fun audioInputStream(): InputStream {
        val audioStreamUrls = this.readAudioStreamUrls()
        var allBytes = byteArrayOf()
        audioStreamUrls.forEach { audioUrl ->
            val bytes = retryOnError { audioUrl.openStreamToResource().use { it.readAllBytes() } }
            allBytes += bytes
        }
        return allBytes.inputStream()
    }

    @Throws(IOException::class)
    fun readVideoStreamUrls(): List<URL> {
        val baseStreamUri = masterM3u8Entry.streamFileURL.toURI()
        val tsSegmentFileAsString = retryOnError {
            masterM3u8Entry.streamFileURL.openStreamToResource().use { String(it.readAllBytes()) }
        }
        return extractAllTsFilesSegments(tsSegmentFileAsString).map { baseStreamUri.resolve(it).toURL() }
    }

    @Throws(IOException::class)
    fun videoInputStream(): InputStream {
        val videoStreamUrls = this.readVideoStreamUrls()
        var allBytes = byteArrayOf()
        videoStreamUrls.forEach { videoUrl ->
            val bytes = retryOnError { videoUrl.openStreamToResource().use { it.readAllBytes() } }
            allBytes += bytes
        }
        return allBytes.inputStream()
    }

    private fun extractSubtitleSegment(content: String): String? {
        return getSubtitleSegmentRegex.find(content)?.value
    }

    private fun extractAllTsFilesSegments(content: String): List<String> {
        return getAllTsSegmentsRegex.findAll(content).asIterable().map { it.value }
    }

    companion object {
        private val getAllTsSegmentsRegex = "(.+ts)".toRegex()
        private val getSubtitleSegmentRegex = "(.+\\.vtt.+)".toRegex()
    }
}