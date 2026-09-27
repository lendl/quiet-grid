package com.quietgrid.cli.wordsearch

import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import com.quietgrid.engine.wordsearch.wordSearchAllowedSizes
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import kotlin.random.Random

fun wordSearchThemeDeficits(bank: List<WordSearchPuzzleEntry>, locale: String, themeIds: List<String>, target: Int): Map<String, Int> {
    val counts = bank.filter { it.locale == locale }.groupingBy { it.themeId }.eachCount()
    return themeIds.associateWith { target - (counts[it] ?: 0) }.filterValues { it > 0 }
}

fun generateWordSearchForQuotas(
    difficulty: Difficulty,
    locale: String,
    quotas: Map<String, Int>,
    threads: Int,
    maxAttempts: Int,
    isNew: (WordSearchPuzzleEntry) -> Boolean = { true },
): List<WordSearchPuzzleEntry> {
    val knownThemeIds = loadSharedThemes().getValue(locale).map { it.themeId }.toSet()
    require(quotas.keys.all { it in knownThemeIds }) { "Unknown wordsearch themes for '$locale': ${quotas.keys - knownThemeIds}" }

    val remaining = quotas.filterValues { it > 0 }.toMutableMap()
    val sizes = wordSearchAllowedSizes(difficulty)
    val accepted = mutableListOf<WordSearchPuzzleEntry>()
    val acceptedIds = mutableSetOf<String>()
    val executor = Executors.newFixedThreadPool(threads)
    val completion = ExecutorCompletionService<WordSearchPuzzleEntry?>(executor)
    var submitted = 0
    var pending = 0

    fun submitNext() {
        val themeId = pickWeightedTheme(remaining)
        val (rows, cols) = sizes.random()
        completion.submit(Callable { generateWordSearchPuzzle(rows, cols, difficulty, listOf(locale), themeId) })
        submitted += 1
        pending += 1
    }

    try {
        while (pending < threads && submitted < maxAttempts && remaining.isNotEmpty()) submitNext()
        while (pending > 0) {
            val entry = completion.take().get()
            pending -= 1
            val stillNeeded = entry != null && (remaining[entry.themeId] ?: 0) > 0
            if (entry != null && stillNeeded && entry.id !in acceptedIds && isNew(entry)) {
                accepted += entry
                acceptedIds += entry.id
                val left = remaining.getValue(entry.themeId) - 1
                if (left == 0) remaining.remove(entry.themeId) else remaining[entry.themeId] = left
            }
            if (submitted < maxAttempts && remaining.isNotEmpty()) submitNext()
        }
    } finally {
        executor.shutdownNow()
    }
    return accepted
}

private fun pickWeightedTheme(remaining: Map<String, Int>): String {
    var ticket = Random.nextInt(remaining.values.sum())
    for ((themeId, count) in remaining) {
        if (ticket < count) return themeId
        ticket -= count
    }
    return remaining.keys.last()
}
