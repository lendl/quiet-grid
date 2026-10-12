package com.quietgrid.cli.sudoku

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuGrade
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.gradeSudoku

data class SudokuRegradeChange(val id: String, val from: String, val to: String?, val grade: SudokuGrade?)

data class SudokuRegradeResult(val kept: List<SudokuPuzzleEntry>, val changes: List<SudokuRegradeChange>)

fun regradeSudokuEntries(
    entries: List<SudokuPuzzleEntry>,
    grader: (SudokuGrid) -> SudokuGrade? = { gradeSudoku(it) },
): SudokuRegradeResult {
    val kept = mutableListOf<SudokuPuzzleEntry>()
    val changes = mutableListOf<SudokuRegradeChange>()
    for (entry in entries) {
        val grade = grader(entry.givens)
        val newKey = grade?.difficulty?.key
        if (newKey != null) kept += entry.copy(difficulty = newKey)
        if (newKey != entry.difficulty) changes += SudokuRegradeChange(entry.id, entry.difficulty, newKey, grade)
    }
    return SudokuRegradeResult(kept, changes)
}

private fun tierCounts(entries: List<SudokuPuzzleEntry>): String =
    Difficulty.entries.joinToString(" ") { tier -> "${tier.key}=${entries.count { it.difficulty == tier.key }}" }

fun runSudokuRegrade(path: String, write: Boolean, grader: (SudokuGrid) -> SudokuGrade? = { gradeSudoku(it) }) {
    val entries = readPuzzleEntries(path, SudokuPuzzleEntry.serializer())
    val result = regradeSudokuEntries(entries, grader)
    val (moved, removed) = result.changes.partition { it.to != null }

    println("before: ${tierCounts(entries)}")
    println("after:  ${tierCounts(result.kept)}")
    println("moved (${moved.size}):")
    moved.forEach { println("  ${it.id} ${it.from} -> ${it.to}") }
    println("removed (${removed.size}):")
    removed.forEach { change ->
        val reason = change.grade?.let { "${it.requiredStep}, ${it.genuineUses} uses" } ?: "unsolvable"
        println("  ${change.id} ${change.from} ($reason)")
    }

    if (write) {
        writePuzzleEntries(path, result.kept, SudokuPuzzleEntry.serializer())
        println("wrote $path")
    }
}
