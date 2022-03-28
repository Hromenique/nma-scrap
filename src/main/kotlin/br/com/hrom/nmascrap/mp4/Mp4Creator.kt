package br.com.hrom.nmascrap.mp4

import java.io.File
import java.nio.file.Path
import kotlin.jvm.Throws

interface Mp4Creator {
    @Throws(CreateMp4FileException::class, IllegalArgumentException::class)
    fun create(videoSourceFile: File, audioSourceFile: File, destMp4FilePath: Path): File

    companion object {
        fun getInstance(): Mp4Creator {
            val process = Runtime.getRuntime().exec("ffmpeg -version")
            process.waitFor()
            val output = process.inputStream.use { input -> String(input.readAllBytes()) }

            return if(output.contains("ffmpeg version")){
                FFMpegMp4Creator()
            }else {
                NoOpMp4Creator()
            }
        }
    }
}