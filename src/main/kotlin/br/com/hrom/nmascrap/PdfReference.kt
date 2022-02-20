package br.com.hrom.nmascrap

import java.net.URL

/**
 * Represents some pdf in NMA
 */
data class PdfReference(val name: String, val url: URL) {
    constructor(url: URL): this(name = url.fileName(), url = url)
}