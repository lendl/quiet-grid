package com.quietgrid.cli.wordguess

import com.quietgrid.engine.wordguess.wordGuessDictionarySerializer
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

fun mergeWordGuessDictionary(dictionary: Map<String, List<String>>, locale: String, words: Collection<String>): Map<String, List<String>> {
    val existing = dictionary[locale].orEmpty()
    val known = existing.toSet()
    return dictionary + (locale to existing + words.distinct().filter { it !in known })
}

fun appendWordGuessDictionary(path: String, locale: String, words: Collection<String>) {
    val file = File(path)
    val existing = if (file.exists()) json.decodeFromString(wordGuessDictionarySerializer, file.readText()) else emptyMap()
    file.parentFile?.mkdirs()
    file.writeText(json.encodeToString(wordGuessDictionarySerializer, mergeWordGuessDictionary(existing, locale, words)))
}
