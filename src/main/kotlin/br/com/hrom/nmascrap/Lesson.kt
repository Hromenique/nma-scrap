package br.com.hrom.nmascrap

data class Lesson(
    val name: String,
    val details: String,
    val pdfReferences: List<PdfReference>,
    val imageReferences: List<ImageReference>,
    val videoReferences: List<VideoReference>
)