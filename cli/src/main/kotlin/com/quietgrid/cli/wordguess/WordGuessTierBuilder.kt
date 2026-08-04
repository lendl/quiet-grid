package com.quietgrid.cli.wordguess

const val WORDGUESS_COMMON_TIER_SIZE = 400
const val WORDGUESS_FULL_TIER_SIZE = 3000

data class WordGuessTieredWords(
    val common: List<String>,
    val full: List<String>,
    val dictionary: Set<String>,
)

fun buildWordGuessTiers(
    rawWords: List<String>,
    wordLength: Int,
    commonSize: Int = WORDGUESS_COMMON_TIER_SIZE,
    fullSize: Int = WORDGUESS_FULL_TIER_SIZE,
): WordGuessTieredWords {
    val lettersOnly = Regex("\\p{L}+")
    val filtered = rawWords
        .asSequence()
        .filter { it.length == wordLength && it.isNotEmpty() && lettersOnly.matches(it) }
        .distinct()
        .toList()

    return WordGuessTieredWords(
        common = filtered.take(commonSize),
        full = filtered.take(fullSize),
        dictionary = filtered.toSet(),
    )
}
