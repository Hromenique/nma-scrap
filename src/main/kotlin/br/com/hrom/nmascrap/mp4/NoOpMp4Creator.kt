package br.com.hrom.nmascrap.mp4

import java.io.File
import java.nio.file.Path

class NoOpMp4Creator : Mp4Creator {

    override fun create(videoSourceFile: File, audioSourceFile: File, destMp4FilePath: Path): File {
        throw CreateMp4FileException("It's not possible create the file $destMp4FilePath. Checks if the runtime environment has ffmpeg installed")
    }
}
