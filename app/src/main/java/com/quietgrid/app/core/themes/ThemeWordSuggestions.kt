package com.quietgrid.app.core.themes

import java.text.Normalizer
import java.util.Locale

const val MAX_THEME_WORD_CHANGES = 40

private const val MIN_SUGGESTED_WORD_LENGTH = 3

private val combiningMarks = Regex("\\p{Mn}+")

private val suggestionSeparators = Regex("[,;\\r\\n]+")

data class WordSuggestions(
    val added: List<String>,
    val duplicates: List<String>,
    val invalid: List<String>,
)

fun foldThemeWord(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)

fun matchesThemeWordSearch(word: String, query: String): Boolean = foldThemeWord(word).contains(foldThemeWord(query))

fun parseWordSuggestions(input: String, existing: List<String>): WordSuggestions {
    val known = existing.mapTo(HashSet()) { foldThemeWord(it) }
    val seen = HashSet<String>()
    val added = mutableListOf<String>()
    val duplicates = mutableListOf<String>()
    val invalid = mutableListOf<String>()
    input.split(suggestionSeparators)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { word ->
            val folded = foldThemeWord(word)
            when {
                word.length < MIN_SUGGESTED_WORD_LENGTH || !word.all { it.isLetter() } -> invalid += word
                folded in known -> duplicates += word
                seen.add(folded) -> added += word
            }
        }
    return WordSuggestions(added, duplicates, invalid)
}
