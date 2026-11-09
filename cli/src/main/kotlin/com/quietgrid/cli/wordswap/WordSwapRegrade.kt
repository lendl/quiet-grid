package com.quietgrid.cli.wordswap

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.countWordSwapArrangements
import com.quietgrid.engine.wordswap.gradeWordSwapPuzzle
import com.quietgrid.engine.wordswap.words

data class WordSwapTierMove(val id: String, val from: String, val to: String)

data class WordSwapUntiered(val id: String, val broken: Boolean)

data class WordSwapRegradeReport(
    val kept: List<WordSwapPuzzleEntry>,
    val moved: List<WordSwapTierMove>,
    val untiered: List<WordSwapUntiered>,
)

fun regradeWordSwapBank(bank: List<WordSwapPuzzleEntry>): WordSwapRegradeReport {
    val kept = mutableListOf<WordSwapPuzzleEntry>()
    val moved = mutableListOf<WordSwapTierMove>()
    val untiered = mutableListOf<WordSwapUntiered>()
    bank.forEach { puzzle ->
        val tier = gradeWordSwapPuzzle(puzzle)?.key
        if (tier == null) {
            val broken = countWordSwapArrangements(puzzle.solution, puzzle.cols, puzzle.slots, puzzle.words()) >= 2
            untiered += WordSwapUntiered(puzzle.id, broken)
            return@forEach
        }
        if (tier != puzzle.difficulty) moved += WordSwapTierMove(puzzle.id, puzzle.difficulty, tier)
        kept += puzzle.copy(difficulty = tier)
    }
    return WordSwapRegradeReport(kept, moved, untiered)
}

fun runWordSwapRegrade(path: String, write: Boolean) {
    val bank = readPuzzleEntries(path, WordSwapPuzzleEntry.serializer())
    val report = regradeWordSwapBank(bank)
    Difficulty.entries.forEach { difficulty ->
        val before = bank.count { it.difficulty == difficulty.key }
        val after = report.kept.count { it.difficulty == difficulty.key }
        println("${difficulty.key}: $before -> $after")
    }
    report.moved.groupingBy { "${it.from} -> ${it.to}" }.eachCount().forEach { (move, count) -> println("moved $move: $count") }
    report.moved.forEach { println("  ${it.id}: ${it.from} -> ${it.to}") }
    println("untiered: ${report.untiered.size} (broken ${report.untiered.count { it.broken }}, solver gap or outside bands ${report.untiered.count { !it.broken }})")
    report.untiered.forEach { println("  ${it.id}: ${if (it.broken) "broken" else "outside bands"}") }
    if (!write) return
    writePuzzleEntries(path, report.kept, WordSwapPuzzleEntry.serializer())
    println("Wrote ${report.kept.size} puzzles to $path")
}
