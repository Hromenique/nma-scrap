package br.com.hrom.nmascrap.m3u8

import java.net.URL

class MasterM3u8Resolver {

    private val subtitleElementExtractor = SubtitleElementExtractor()
    private val audioElementExtractor = AudioElementExtractor()
    private val streamElementExtractor = StreamElementExtractor()

    fun resolve(m3u8FileContent: String): MasterM3u8 {
        val subtitleElementsByGroupId = subtitleElementExtractor.extract(m3u8FileContent).associateBy { it.groupId }
        val audioElementsByGroupID = audioElementExtractor.extract(m3u8FileContent).associateBy { it.groupId }
        val streamElements = streamElementExtractor.extract(m3u8FileContent)

        val m3u8MasterGroups = streamElements.map { streamElement ->
            val streamUrl = streamElement.url
            val streamVideoResolution = streamElement.resolution?.let { VideoResolution.of(it) }
            val subtitleUrl = streamElement.subtitle?.let { subtitleElementsByGroupId[it] }?.url ?: subtitleElementsByGroupId.values.firstOrNull()?.url
            val audioUrl = streamElement.audio?.let { audioElementsByGroupID[it] }?.url ?: audioElementsByGroupID.values.firstOrNull()?.url
            MasterM3u8Entry(streamUrl, subtitleUrl, audioUrl, streamVideoResolution)
        }

        return MasterM3u8(m3u8MasterGroups)
    }
}

internal class StreamElementExtractor() {
    fun extract(content: String): List<StreamElement> {
        return getAllStreamGroupsRegex.findAll(content)
            .asIterable()
            .map { match ->
                val subtitleGroupId = getSubtitleGroupIdRegex.find(match.value)?.destructured?.component1()
                val audioGroupId = getAudioGroupIdRegex.find(match.value)?.destructured?.component1()
                val resolution = getResolutionRegex.find(match.value)?.destructured?.component1()
                val (url) = getUriRegex.find(match.value)?.destructured
                    ?: throw IllegalStateException("URI was not found in STREAM")
                StreamElement(subtitleGroupId, resolution, audioGroupId, URL(url))
            }
    }

    companion object {
        private val getAllStreamGroupsRegex = "(#EXT-X-STREAM.+\\n.+)".toRegex()
        private val getSubtitleGroupIdRegex = "SUBTITLES=\"([A-Za-z0-9/-]+)\"".toRegex()
        private val getAudioGroupIdRegex = "AUDIO=\"([A-Za-z0-9/-]+)\"".toRegex()
        private val getResolutionRegex = "RESOLUTION=([0-9x]+)".toRegex()
        private val getUriRegex = "#EXT-X-STREAM.+\\n(.+)".toRegex()
    }
}

internal class AudioElementExtractor() {

    @Throws(IllegalStateException::class)
    fun extract(content: String): List<AudioElement> {
        return getAllAudioLinesRegex.findAll(content)
            .asIterable()
            .map { match ->
                val (groupId) = getGroupIdRegex.find(match.value)?.destructured
                    ?: throw IllegalStateException("GROUP-ID in AUDIO was not found")
                val (url) = getUriRegex.find(match.value)?.destructured
                    ?: throw IllegalStateException("URI in AUDIO was not found")
                AudioElement(groupId, URL(url))
            }
    }

    companion object {
        private val getAllAudioLinesRegex = "^(#EXT-X-MEDIA:TYPE=AUDIO.+)$".toRegex(RegexOption.MULTILINE)
        private val getGroupIdRegex = "GROUP-ID=\"([A-Za-z0-9/-]+)\"".toRegex()
        private val getUriRegex = "URI=\"(.+?)\"".toRegex()
    }
}

internal class SubtitleElementExtractor() {

    @Throws(IllegalStateException::class)
    fun extract(content: String): List<SubtitleElement> {
        return getAllSubtitleLinesRegex.findAll(content)
            .asIterable()
            .map { match ->
                val (groupId) = getGroupIdRegex.find(match.value)?.destructured
                    ?: throw IllegalStateException("GROUP-ID in SUBTITLES was not found")
                val (uri) = getUriRegex.find(match.value)?.destructured
                    ?: throw IllegalStateException("URI in SUBTITLES was not found")
                SubtitleElement(groupId, URL(uri))
            }
    }

    companion object {
        private val getAllSubtitleLinesRegex = "^(#EXT-X-MEDIA:TYPE=SUBTITLES.+)$".toRegex(RegexOption.MULTILINE)
        private val getGroupIdRegex = "GROUP-ID=\"([A-Za-z0-9/-]+)\"".toRegex()
        private val getUriRegex = "URI=\"(.+?)\"".toRegex()
    }
}

internal data class StreamElement(val subtitle: String?, val resolution: String?, val audio: String?, val url: URL)

internal data class AudioElement(val groupId: String, val url: URL)

internal data class SubtitleElement(val groupId: String, val url: URL)