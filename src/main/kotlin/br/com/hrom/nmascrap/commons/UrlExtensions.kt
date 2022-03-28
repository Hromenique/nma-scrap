package br.com.hrom.nmascrap.commons

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.jvm.Throws

internal val GET_EXTENSION_REGEX = ".*\\.(?<extension>.+)\$".toRegex()

fun URL.fileName(): String = this.file.substring(this.file.lastIndexOf('/') + 1, this.file.length)

fun URL.fileExtension(): String? = GET_EXTENSION_REGEX.find(this.path)?.destructured?.component1()

@Throws(IOException::class)
fun URL.openStreamToResource(
    origin: String = "https://www.nma.art",
    refererHeaderValue: String? = null
): InputStream {
    val urlConnection = this.openConnection() as HttpURLConnection

    urlConnection.setRequestProperty("Origin", origin)
    refererHeaderValue?.let { urlConnection.setRequestProperty("Referer", refererHeaderValue) }

    urlConnection.doOutput = true
    urlConnection.doInput = true
    return urlConnection.inputStream
}

