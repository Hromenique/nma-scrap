package br.com.hrom.nmascrap.commons

import java.io.File

fun File.deleteAndCreateNew(): File {
    if(this.exists()) this.delete()
    this.createNewFile()
    return this
}