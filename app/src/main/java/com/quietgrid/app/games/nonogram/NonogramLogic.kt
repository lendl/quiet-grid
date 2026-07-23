package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.Difficulty
import kotlin.math.max

private const val MAX_SCORE = 10_000
private const val MIN_SCORE = 1_000

fun buildNonogramClues(line: List<Boolean>): List<Int> {
    val clues = mutableListOf<Int>()
    var run = 0
    for (cell in line) {
        if (cell) {
            run += 1
        } else if (run > 0) {
            clues.add(run)
            run = 0
        }
    }
    if (run > 0) clues.add(run)
    return if (clues.isNotEmpty()) clues else listOf(0)
}

private fun buildClueSet(solution: List<List<Boolean>>): Pair<List<List<Int>>, List<List<Int>>> {
    val rowClues = solution.map { buildNonogramClues(it) }
    val cols = solution.firstOrNull()?.size ?: 0
    val colClues = (0 until cols).map { c -> buildNonogramClues(solution.map { it[c] }) }
    return rowClues to colClues
}

fun buildNonogramPuzzle(entry: NonogramPuzzleEntry): NonogramPuzzle {
    val (rowClues, colClues) = buildClueSet(entry.solution)
    return NonogramPuzzle(
        id = entry.id,
        difficulty = entry.difficulty,
        rows = entry.rows,
        cols = entry.cols,
        rowClues = rowClues,
        colClues = colClues,
    )
}

fun createNonogramSession(entry: NonogramPuzzleEntry): NonogramSession = NonogramSession(
    puzzle = buildNonogramPuzzle(entry),
    board = List(entry.rows) { List(entry.cols) { null } },
    solution = entry.solution,
)

/** True if the filled/empty run-lengths in [line] exactly match [clues] (position-agnostic shape check). */
fun isNonogramLineComplete(line: List<NonogramCellValue>, clues: List<Int>): Boolean {
    val segments = mutableListOf<Int>()
    var run = 0
    for (cell in line) {
        if (cell == 1) {
            run += 1
        } else if (run > 0) {
            segments.add(run)
            run = 0
        }
    }
    if (run > 0) segments.add(run)
    return segments.size == clues.size && segments.indices.all { segments[it] == clues[it] }
}

private fun isLineCorrectlyComplete(cells: List<NonogramCellValue>, clues: List<Int>, solutionLine: List<Boolean>): Boolean =
    isNonogramLineComplete(cells, clues) && cells.indices.all { cells[it] != 1 || solutionLine[it] }

private fun autoFillCompletedLines(board: List<MutableList<NonogramCellValue>>, puzzle: NonogramPuzzle, solution: List<List<Boolean>>) {
    puzzle.rowClues.forEachIndexed { rowIndex, clues ->
        val cells = board[rowIndex]
        val solutionRow = solution[rowIndex]
        if (isLineCorrectlyComplete(cells, clues, solutionRow)) {
            for (c in cells.indices) if (cells[c] == null) cells[c] = 0
        }
    }
    puzzle.colClues.forEachIndexed { colIndex, clues ->
        val cells = board.map { it[colIndex] }
        val solutionCol = solution.map { it[colIndex] }
        if (isLineCorrectlyComplete(cells, clues, solutionCol)) {
            for (r in board.indices) if (board[r][colIndex] == null) board[r][colIndex] = 0
        }
    }
}

private fun cycleForMode(value: NonogramCellValue, mode: NonogramInputMode): NonogramCellValue = when (mode) {
    NonogramInputMode.FILL -> if (value == 1) null else 1
    NonogramInputMode.CROSS -> if (value == 0) null else 0
}

/** Tap cycles a single cell: blank -> target -> blank, per mode. Returns null if unchanged. */
fun applyNonogramTap(session: NonogramSession, row: Int, col: Int, mode: NonogramInputMode): NonogramSession? {
    val current = session.board[row][col]
    val next = cycleForMode(current, mode)
    if (next == current) return null

    val board = session.board.map { it.toMutableList() }.toMutableList()
    board[row][col] = next
    autoFillCompletedLines(board, session.puzzle, session.solution)
    return session.copy(board = board)
}

/** Drag-paint sets every listed cell to [value] uniformly (no toggling). Returns null if nothing changed. */
fun applyNonogramPaint(session: NonogramSession, cells: List<Pair<Int, Int>>, value: Int): NonogramSession? {
    val board = session.board.map { it.toMutableList() }.toMutableList()
    var changed = false
    val seen = mutableSetOf<Pair<Int, Int>>()
    for ((row, col) in cells) {
        if (!seen.add(row to col)) continue
        if (board[row][col] != value) {
            board[row][col] = value
            changed = true
        }
    }
    if (!changed) return null
    autoFillCompletedLines(board, session.puzzle, session.solution)
    return session.copy(board = board)
}

fun isNonogramSolved(board: NonogramGrid, solution: List<List<Boolean>>): Boolean =
    board.indices.all { r -> board[r].indices.all { c -> if (solution[r][c]) board[r][c] == 1 else board[r][c] != 1 } }

fun nonogramHasMeaningfulProgress(session: NonogramSession): Boolean =
    session.board.any { row -> row.any { it != null } }

fun nonogramScore(elapsedSeconds: Int): Int = max(MIN_SCORE, MAX_SCORE - elapsedSeconds * 10)
