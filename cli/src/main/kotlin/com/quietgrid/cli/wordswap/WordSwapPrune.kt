package com.quietgrid.cli.wordswap

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.words

fun pruneWordSwapBank(bank: List<WordSwapPuzzleEntry>, allowed: Set<String>): List<WordSwapPuzzleEntry> =
    bank.filter { puzzle -> puzzle.words().all { it in allowed } }

fun runWordSwapPrune(locale: String, outDir: String) {
    val path = "$outDir/${wordSwapBankFile(locale)}"
    val bank = readPuzzleEntries(path, WordSwapPuzzleEntry.serializer())
    val kept = pruneWordSwapBank(bank, loadWordSwapRankedWords(locale).toHashSet())
    writePuzzleEntries(path, kept, WordSwapPuzzleEntry.serializer())
    println("Pruned $locale: kept ${kept.size} of ${bank.size} puzzles in $path")
}
