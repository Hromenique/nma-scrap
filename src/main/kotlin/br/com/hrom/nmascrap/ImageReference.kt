package br.com.hrom.nmascrap

import java.net.URL

private val REGEX_FILE_EXTENSION = ".+\\.(?<extension>.+)".toRegex()

data class ImageReference(val name: String, val url: URL, val format: String) {
    constructor(url: URL) : this(
        name = url.fileName(),
        url = url,
        format = REGEX_FILE_EXTENSION.find(url.file)?.destructured?.component1() ?: "jpg"
    )
}