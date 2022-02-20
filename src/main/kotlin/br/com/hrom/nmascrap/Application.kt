package br.com.hrom.nmascrap

import java.io.File
import java.net.URL


fun main() {
    val sysProperties = System.getProperties();
    // set this to allow to send http 'Origin' header to handle cross domain problems
    sysProperties.setProperty("sun.net.http.allowRestrictedHeaders", "true")


    println("Welcome to NMA Scrap Application. Please provide the necessary information to continue...")

    println("User Name:")
    val username = readLine() ?: ""

    println("Password:")
    val password = readLine() ?: ""

    println("Lesson URL:")
    val lessonUrl = readLine(errorMessage = "URL malformed, try again") { URL(it) }

    println("Preferred video resolution: SD, HD, FULL-HD (default is HD): ")
    val resolution = readOptions(
        options = listOf("SD", "HD", "FULL_HD"),
        default = "HD",
        errorMessage = "Wrong choice, try again"
    ) { Resolution.valueOf(it) }

    println("Destination folder:")
    val destinationFolder = readLine(errorMessage = "Folder does not exists, try again") {
        val folder = File(it)
        if (!folder.exists()) throw IllegalArgumentException()
        folder
    }

    NMAScraper(
        userName = username,
        password = password,
        lessonUrl = lessonUrl,
        destinationFolder = destinationFolder,
        preferredResolution = resolution
    ).doScrap()

}