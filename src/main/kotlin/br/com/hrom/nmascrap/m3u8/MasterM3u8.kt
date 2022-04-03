package br.com.hrom.nmascrap.m3u8

import br.com.hrom.nmascrap.scraper.Resolution

class MasterM3u8(entries: List<MasterM3u8Entry>) {
    private val entries: List<MasterM3u8Entry>

    init {
        this.entries = entries.sortedBy { it.videoResolution }
    }

    fun getEntryWithResolutionOrDefault(resolution: Resolution): MasterM3u8Entry? {
        return this.entries.firstOrNull { it.videoResolution?.toResolution() == resolution } ?: this.entries.lastOrNull()
    }
}