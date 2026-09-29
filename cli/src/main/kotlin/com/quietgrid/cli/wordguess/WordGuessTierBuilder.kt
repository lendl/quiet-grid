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
    "de" to setOf('j', 'q', 'v', 'x', 'y'),
    "en" to setOf('b', 'g', 'j', 'k', 'p', 'q', 'v', 'x', 'y', 'z'),
    "es" to setOf('j', 'k', 'w', 'x', 'z'),
    "fr" to setOf('j', 'k', 'w', 'x', 'y', 'z'),
    "nl" to setOf('c', 'q', 'x', 'y'),
    "it" to setOf('j', 'k', 'q', 'w', 'x', 'y', 'z'),
    "pl" to setOf('f', 'h', 'q', 'v', 'x'),
    "pt" to setOf('k', 'w', 'y'),
)

val WORDGUESS_FREQUENCY_RANKED_LOCALES = setOf("de", "en", "es", "fr", "nl", "pt")

fun filterWordGuessByDictionary(ranked: List<String>, dictionary: List<String>): List<String> {
    val valid = dictionary.mapTo(HashSet()) { asciiFoldWord(it).lowercase() }
    return ranked.filter { asciiFoldWord(it).lowercase() in valid }
}

fun loadWordGuessBlocklist(locale: String): Set<String> =
    object {}.javaClass.getResourceAsStream("/wordguess_blocklist_$locale.txt")
        ?.bufferedReader()
        ?.useLines { lines -> lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet() }
        .orEmpty()

fun removeWordGuessBlocklisted(words: List<String>, blocklist: Set<String>): List<String> =
    words.filter { asciiFoldWord(it).lowercase() !in blocklist }

private const val WORDGUESS_SHUFFLE_SEED = 20260912L

fun wideWordGuessPool(tiers: WordGuessTieredWords): List<String> {
    val common = tiers.common.toSet()
    return tiers.full.filter { it !in common }.shuffled(java.util.Random(WORDGUESS_SHUFFLE_SEED))
}

fun sortWordGuessByRarity(words: List<String>, locale: String): List<String> {
    val rareLetters = WORDGUESS_RARE_LETTERS[locale].orEmpty()
    val shuffled = words.shuffled(java.util.Random(WORDGUESS_SHUFFLE_SEED))
    return shuffled.sortedBy { word -> asciiFoldWord(word).lowercase().count { it in rareLetters } }
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
