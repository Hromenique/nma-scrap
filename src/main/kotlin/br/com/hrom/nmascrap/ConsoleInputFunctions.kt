package br.com.hrom.nmascrap

fun <T> readLine(errorMessage: String, mapper: (String) -> T): T {
    do {
        try {
            val value = readLine() ?: ""
            return mapper(value)
        } catch (ex: Exception) {
            println(errorMessage)
        }
    } while (true)
}

fun <T> readOptions(
    options: List<String>,
    default: String? = null,
    errorMessage: String,
    mapper: (String) -> T,
): T {
    do {
        val value = readLine()?.trim() ?: ""

        if (value in options) {
            return mapper(value)
        }

        if (default != null && value.isBlank()) {
            return mapper(default)
        }

        println(errorMessage)
    } while (true)
}