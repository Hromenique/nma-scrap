package br.com.hrom.nmascrap

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.lang.IllegalStateException
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class VideoReference(
    val title: String,
    val sources: List<Source> = emptyList(),
    val tracks: List<Track> = emptyList()
) {
    fun tryGetSourceWithResolution(resolution: Resolution): Source {
        return sources.firstOrNull { it.resolution == resolution}
            ?: sources.firstOrNull()
            ?: throw IllegalStateException("There is no source video")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Source(
    @JsonProperty("label")
    @JsonDeserialize(using = ResolutionDeserializer::class)
    val resolution: Resolution = Resolution.UNKNOWN,
    @JsonProperty("file")
    val url: URL,
    val type: String
) {
    val extension: String? = url.fileExtension()    //type.substring(type.lastIndexOf('/') + 1, type.length)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Track(
    @JsonProperty("file")
    val url: URL,
    val kind: String
){
    val extension: String? = url.fileExtension()
}

enum class Resolution {
    FULL_HD, HD, SD, UNKNOWN;
}

class ResolutionDeserializer : StdDeserializer<Resolution>(Resolution::class.java) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Resolution {
        return when (parser.text?.toUpperCase()) {
            "FULL-HD" -> Resolution.FULL_HD
            "HD" -> Resolution.HD
            "SD" -> Resolution.SD
            else -> Resolution.UNKNOWN
        }
    }
}