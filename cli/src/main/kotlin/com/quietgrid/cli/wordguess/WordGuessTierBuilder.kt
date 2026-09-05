package com.quietgrid.cli.wordguess

import java.text.Normalizer

const val WORDGUESS_COMMON_TIER_SIZE = 400
const val WORDGUESS_FULL_TIER_SIZE = 3000

data class WordGuessTieredWords(
    val common: List<String>,
    val full: List<String>,
    val dictionary: Set<String>,
)

private val combiningMarks = Regex("\\p{Mn}+")
private val asciiLettersOnly = Regex("[a-z]+")

fun asciiFoldWord(word: String): String {
    val substituted = word.replace("ß", "ss").replace("ł", "l").replace("Ł", "L")
    val decomposed = Normalizer.normalize(substituted, Normalizer.Form.NFD)
    return combiningMarks.replace(decomposed, "")
}

val WORDGUESS_RARE_LETTERS: Map<String, Set<Char>> = mapOf(
    "en" to setOf('b', 'g', 'j', 'k', 'p', 'q', 'v', 'x', 'y', 'z'),
    "es" to setOf('j', 'k', 'w', 'x', 'z'),
    "fr" to setOf('j', 'k', 'w', 'x', 'y', 'z'),
    "nl" to setOf('c', 'q', 'x', 'y'),
    "pl" to setOf('f', 'h', 'q', 'v', 'x'),
)

fun sortWordGuessByRarity(words: List<String>, locale: String): List<String> {
    val rareLetters = WORDGUESS_RARE_LETTERS[locale].orEmpty()
    return words.sortedWith(
        compareBy<String> { word -> asciiFoldWord(word).lowercase().count { it in rareLetters } }
            .thenBy { it }
    )
}

fun buildWordGuessTiers(
    rawWords: List<String>,
    wordLength: Int,
    commonSize: Int = WORDGUESS_COMMON_TIER_SIZE,
    fullSize: Int = WORDGUESS_FULL_TIER_SIZE,
): WordGuessTieredWords {
    val filtered = rawWords
        .asSequence()
        .map { asciiFoldWord(it) }
        .filter { it.length == wordLength && it.isNotEmpty() && asciiLettersOnly.matches(it) }
        .distinct()
        .toList()

    return WordGuessTieredWords(
        common = filtered.take(commonSize),
        full = filtered.take(fullSize),
        dictionary = filtered.toSet(),
    )
}
