package br.com.hrom.nmascrap.m3u8

import br.com.hrom.nmascrap.commons.openStreamToResource
import java.io.InputStream
import java.net.URL

class UrlPlaylistInputStream(private val urls: List<URL>) : InputStream() {
    private var currentUrlIndex = 0
    private var currentInputStream =
        if (urls.isNotEmpty()) urls[currentUrlIndex].openStreamToResource() else nullInputStream()

    override fun read(): Int {
        val nextByte = currentInputStream.read()

        if (nextByte == -1) {
            currentUrlIndex++

            if (currentUrlIndex >= urls.size) {
                return -1
            }

            currentInputStream = urls[currentUrlIndex].openStreamToResource()
            return read()
        }

        return nextByte
    }
}