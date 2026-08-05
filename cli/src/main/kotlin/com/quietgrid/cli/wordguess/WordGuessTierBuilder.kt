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

/** Folds a word down to plain a-z: ß -> ss, then strips accents via NFD decomposition. */
fun asciiFoldWord(word: String): String {
    val deSharpened = word.replace("ß", "ss")
    val decomposed = Normalizer.normalize(deSharpened, Normalizer.Form.NFD)
    return combiningMarks.replace(decomposed, "")
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
