package com.quietgrid.cli.wordswap

import com.quietgrid.cli.wordguess.hunspellParadigmsContaining
import com.quietgrid.cli.wordguess.loadWordGuessBlocklist
import com.quietgrid.cli.wordguess.loadWordGuessFrequencyWords
import com.quietgrid.cli.wordguess.loadWordGuessHunspellWords

val WORDSWAP_LOCALES: List<String> = listOf("en", "de", "es", "fr", "it", "nl", "pl", "pt")

private val plainWord = Regex("[a-z]{3,7}")

private val vowel = Regex("[aeiouy]")

private val cachedRanked = HashMap<String, List<String>>()

fun loadWordSwapRankedWords(locale: String): List<String> = synchronized(cachedRanked) {
    cachedRanked.getOrPut(locale) {
        val dictionary = loadWordGuessHunspellWords(locale).filterTo(HashSet()) { plainWord.matches(it) }
        val listed = loadWordGuessBlocklist(locale) + loadWordSwapBlocklist(locale)
        val blocked = listed + hunspellParadigmsContaining(locale, listed)
        loadWordGuessFrequencyWords(locale)
            .asSequence()
            .filter { plainWord.matches(it) && vowel.containsMatchIn(it) && it in dictionary && it !in blocked }
            .distinct()
            .toList()
    }
}

fun loadWordSwapBlocklist(locale: String): Set<String> =
    object {}.javaClass.getResourceAsStream("/wordswap_blocklist_$locale.txt")
        ?.bufferedReader()
        ?.useLines { lines -> lines.filter { !it.startsWith("#") }.flatMap { it.trim().lowercase().split(Regex("\\s+")) }.filter { it.isNotEmpty() }.toSet() }
        .orEmpty()

class WordSwapLexicon(words: List<String>) {
    private val byLength: Map<Int, List<String>> = words.groupBy { it.length }
    private val index: Map<Int, Array<Array<java.util.BitSet>>> = byLength.mapValues { (length, list) ->
        Array(length) { pos ->
            Array(26) { letter ->
                java.util.BitSet(list.size).also { bits ->
                    list.forEachIndexed { i, w -> if (w[pos] - 'a' == letter) bits.set(i) }
                }
            }
        }
    }

    fun words(length: Int): List<String> = byLength[length].orEmpty()

    fun candidates(pattern: List<Char?>): java.util.BitSet {
        val list = byLength[pattern.size] ?: return java.util.BitSet()
        val result = java.util.BitSet(list.size)
        result.set(0, list.size)
        val positions = index.getValue(pattern.size)
        pattern.forEachIndexed { pos, ch -> if (ch != null) result.and(positions[pos][ch - 'a']) }
        return result
    }
}
