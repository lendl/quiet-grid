package com.quietgrid.cli.wordguess

private val cache = mutableMapOf<String, List<String>>()

fun loadWordGuessFrequencyWords(locale: String): List<String> {
    cache[locale]?.let { return it }
    val text = object {}.javaClass.getResourceAsStream("/wordguess_frequency_$locale.txt")
        ?.bufferedReader()?.readText()
        ?: error("wordguess_frequency_$locale.txt not found on classpath")
    val words = text.lineSequence()
        .mapNotNull { line -> line.trim().substringBefore(' ').takeIf { it.isNotEmpty() } }
        .map { it.lowercase() }
        .toList()
    cache[locale] = words
    return words
}
