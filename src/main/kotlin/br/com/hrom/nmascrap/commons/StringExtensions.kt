package br.com.hrom.nmascrap.commons

private val TO_FOLDER_NAME_REGEX =  "[^0-9a-zA-Z\\s]".toRegex()
private val TO_FILE_NAME_REGEX =  "[^0-9a-zA-Z\\s\\.]".toRegex()


fun String.normalizeToFolderName() = this.replace(TO_FOLDER_NAME_REGEX, " ").toLowerCase()

fun String.normalizeToFileName() = this.replace(TO_FILE_NAME_REGEX, " ").toLowerCase()