package br.com.hrom.nmascrap.scraper

import br.com.hrom.nmascrap.commons.fileName
import java.net.URL

/**
 * Represents some pdf in NMA
 */
data class PdfReference(val name: String, val url: URL) {
    constructor(url: URL): this(name = url.fileName(), url = url)
}