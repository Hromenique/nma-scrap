package br.com.hrom.nmascrap

import java.net.URL

internal val GET_EXTENSION_REGEX = ".*\\.(?<extension>.+)\$".toRegex()

fun URL.fileName(): String = this.file.substring(this.file.lastIndexOf('/') + 1, this.file.length)

fun URL.fileExtension(): String? = GET_EXTENSION_REGEX.find(this.path)?.destructured?.component1()