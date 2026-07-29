package com.quietgrid.cli.wordsearch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SeedTheme(val themeId: String, val words: List<String>)

private var cached: Map<String, List<SeedTheme>>? = null

fun loadWordSearchSeedCorpus(): Map<String, List<SeedTheme>> {
    cached?.let { return it }
    val text = object {}.javaClass.getResourceAsStream("/wordsearch_seed_corpus.json")
        ?.bufferedReader()?.readText()
        ?: error("wordsearch_seed_corpus.json not found on classpath")
    val json = Json { ignoreUnknownKeys = true }
    val parsed = json.decodeFromString<Map<String, List<SeedTheme>>>(text)
    cached = parsed
    return parsed
}
