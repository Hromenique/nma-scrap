package br.com.hrom.nmascrap.scraper

import java.net.URL

/**
 * All pdf in NMA are in an element:
 *
 * ```
 * <p class="viewer-pdf-link">
 *     <img src="https://www.nma.art/wp-content/themes/canvas/images/pdf.svg" alt="PDF icon">
 *     <a href="https://www.nma.art/pdf-viewer?id=1303814" target="_blank">Assignment Week 1 PDF (Light Version)</a>
 * </p>
 * ```
 * This class has all necessary information of that element used to do the scrap
 *
 */
data class PdfViewerReference(val url: URL)