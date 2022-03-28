package br.com.hrom.nmascrap.mp4

import java.io.File
import java.nio.file.Path
import kotlin.jvm.Throws

class FFMpegMp4Creator : Mp4Creator {

    @Throws(CreateMp4FileException::class, IllegalArgumentException::class)
    override fun create(videoSourceFile: File, audioSourceFile: File, destMp4FilePath: Path): File {
        checks(videoSourceFile, audioSourceFile, destMp4FilePath)

        val videoSourcePath = videoSourceFile.absolutePath
        val audioSourcePath = audioSourceFile.absolutePath.toString()
        val destinationPath = destMp4FilePath.toAbsolutePath().toString()

        //"ffmpeg -i $videoSourcePath -i $audioSourcePath -y -c copy $destinationPath"
        val command =
            arrayOf<String>("ffmpeg", "-i", videoSourcePath, "-i", audioSourcePath, "-y", "-c", "copy", destinationPath)

        val process: Process?
        try {
            process = Runtime.getRuntime().exec(command)
            process.waitFor() //blocks
        } catch (ex: Exception) {
            throw CreateMp4FileException("Error when try to create mp4 file", ex)
        }

        val mp4File = destMp4FilePath.toFile()
        if (!mp4File.exists()) {
            val cause = process?.errorStream?.readAllBytes()?.let { String(it) }
            throw CreateMp4FileException("Error when try to create mp4 file: $cause")
        }

        return mp4File
    }

    @Throws(IllegalArgumentException::class)
    private fun checks(videoSourceFile: File, audioSourceFile: File, destMp4FilePath: Path) {
        if (!videoSourceFile.exists() || !videoSourceFile.isFile) {
            throw IllegalArgumentException("videoSourceFile must be a existent file")
        }

        if (!audioSourceFile.exists() || !audioSourceFile.isFile) {
            throw IllegalArgumentException("audioFile must be a existent file")
        }

        if (!destMp4FilePath.fileName.toString().endsWith(".mp4")) {
            throw IllegalArgumentException("destMp4FilePath must be a file ended in .mp4")
        }
    }
}