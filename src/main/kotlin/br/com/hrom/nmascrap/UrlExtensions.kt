package br.com.hrom.nmascrap

import java.net.URL

fun URL.fileName(): String = this.file.substring(this.file.lastIndexOf('/') + 1, this.file.length)