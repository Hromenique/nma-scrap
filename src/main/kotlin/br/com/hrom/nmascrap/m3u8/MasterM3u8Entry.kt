package br.com.hrom.nmascrap.m3u8

import java.net.URL

data class MasterM3u8Entry(
    val streamFileURL: URL,
    val subtitleFileURL: URL?,
    val audioFileURL: URL?,
    val videoResolution: VideoResolution?
) {

    fun toReader() = M3u8PlayListReader(this)

}