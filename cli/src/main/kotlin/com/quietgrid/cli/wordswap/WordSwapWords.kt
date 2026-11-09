package com.quietgrid.cli.wordswap

import com.quietgrid.cli.wordguess.loadWordGuessBlocklist
import com.quietgrid.cli.wordguess.loadWordGuessFrequencyWords
import com.quietgrid.cli.wordguess.loadWordGuessHunspellWords

private val plainWord = Regex("[a-z]{3,7}")

private var cachedRanked: List<String>? = null

fun loadWordSwapRankedWords(): List<String> {
    cachedRanked?.let { return it }
    val dictionary = loadWordGuessHunspellWords("en").filterTo(HashSet()) { plainWord.matches(it) }
    val blocklist = loadWordGuessBlocklist("en")
    val extraBlocked = loadWordSwapExtraBlocklist()
    val ranked = loadWordGuessFrequencyWords("en")
        .asSequence()
        .filter { plainWord.matches(it) && it in dictionary && it !in blocklist && it !in extraBlocked }
        .distinct()
        .toList()
    cachedRanked = ranked
    return ranked
}

private fun loadWordSwapExtraBlocklist(): Set<String> =
    object {}.javaClass.getResourceAsStream("/wordswap_blocklist_en.txt")
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
