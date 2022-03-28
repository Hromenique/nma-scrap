package br.com.hrom.nmascrap.m3u8

import br.com.hrom.nmascrap.scraper.Resolution

/*
,RESOLUTION=1920x1080
RESOLUTION=960x540
ESOLUTION=640x360
ESOLUTION=426x240
RESOLUTION=1280x720
 */

/*
SD = Largura x altura: 640 x 480 pixels ou 720 x 480
HD = Largura x altura: 1280 x 720 pixe
FULL-HD = Nomenclaturas relacionadas: 1080p Largura x altura: 1920 x 1080 pixels


 */
data class VideoResolution(val width: Int, val height: Int) : Comparable<VideoResolution> {

    companion object {
        fun of(str: String): VideoResolution {
            val values = str.toUpperCase().split("X")
            val width = values[0].toInt()
            val height = values[1].toInt()
            return VideoResolution(width, height)
        }
    }

    fun toResolution(): Resolution {
        return when {
            height < 720 -> Resolution.SD
            height in 720..1079 -> Resolution.HD
            height > 1080 -> return Resolution.FULL_HD
            else -> Resolution.UNKNOWN
        }
    }

    override fun toString(): String {
        return "${width}x${height}"
    }

    override fun compareTo(other: VideoResolution): Int {
        return this.height - other.height
    }
}