package br.com.hrom.nmascrap

import br.com.hrom.nmascrap.commons.readOptions
import br.com.hrom.nmascrap.commons.readLine
import br.com.hrom.nmascrap.scraper.NMAScraper
import br.com.hrom.nmascrap.scraper.Resolution
import java.io.File
import java.net.URL


fun main() {
    val sysProperties = System.getProperties();
    // set this to allow sending http 'Origin' header to handle cross domain problems
    sysProperties.setProperty("sun.net.http.allowRestrictedHeaders", "true")

    startScraper()
}

private fun startScraper() {
    println("Welcome to NMA Scrap Application. Please, provide the necessary information to continue...")

    println("User Name:")
    val username = readLine() ?: ""

    println("Password:")
    val password = readLine() ?: ""

    println("Course URL:")
    val courseUrl = readLine(errorMessage = "URL malformed, try again") { URL(it) }

    println("Lesson number (default is all lessons)")
    val lessonNumber: Int? = readLine(errorMessage = "Value must be a positive number") { input ->
        input
            .let { if (input.trim().isEmpty()) null else it }
            ?.toInt()
            ?.let { if (it <= 0) throw IllegalArgumentException("value must be a positive number") else it }
    }

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
        courseUrl = courseUrl,
        lessonNumber = lessonNumber,
        destinationFolder = destinationFolder,
        preferredResolution = resolution
    ).doScrap()
}