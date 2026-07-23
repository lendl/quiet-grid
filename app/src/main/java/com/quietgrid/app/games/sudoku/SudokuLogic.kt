package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.Difficulty
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SCORE = 10_000
private const val MIN_SCORE = 1_000
private const val ACCURACY_SCORE_COST = 500
private const val ACCURACY_DROP_STEP = 10

private val TIME_TO_ZERO_SECONDS = mapOf(
    Difficulty.EASY to 600,
    Difficulty.MEDIUM to 900,
    Difficulty.HARD to 1200,
    Difficulty.EXPERT to 1800,
)

fun sudokuScore(difficulty: Difficulty, timeSeconds: Int, accuracyDrops: Int): Int {
    val timeCap = TIME_TO_ZERO_SECONDS.getValue(difficulty)
    val timePenalty = (max(0, timeSeconds).toDouble() * MAX_SCORE) / timeCap
    val accuracyPenalty = accuracyDrops * ACCURACY_SCORE_COST
    return max(MIN_SCORE, (MAX_SCORE - timePenalty - accuracyPenalty).roundToInt())
}

fun sudokuAccuracyPct(accuracyDrops: Int): Int = max(0, 100 - accuracyDrops * ACCURACY_DROP_STEP)

fun createSudokuSession(entry: SudokuPuzzleEntry): SudokuSession = SudokuSession(
    puzzle = entry,
    board = entry.givens,
    notes = List(9) { List(9) { emptySet() } },
    inputMode = SudokuInputMode.DIGIT,
    accuracyDrops = 0,
    finishedCells = List(9) { List(9) { false } },
    penalizedUnitKeys = emptyList(),
)

private fun isGiven(puzzle: SudokuPuzzleEntry, row: Int, col: Int): Boolean = puzzle.givens[row][col] != null

fun applySudokuSetDigit(session: SudokuSession, row: Int, col: Int, digit: Int): SudokuSession? {
    if (isGiven(session.puzzle, row, col) || session.finishedCells[row][col]) return null
    if (session.board[row][col] == digit) return null

    val board = session.board.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c == col) digit else v } }
    val notes = session.notes.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c == col) emptySet() else v } }
    return session.copy(board = board, notes = notes)
}

fun applySudokuClearCell(session: SudokuSession, row: Int, col: Int): SudokuSession? {
    if (isGiven(session.puzzle, row, col) || session.finishedCells[row][col]) return null
    val hasNotes = session.notes[row][col].isNotEmpty()
    if (session.board[row][col] == null && !hasNotes) return null

    val board = session.board.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c == col) null else v } }
    val notes = session.notes.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c == col) emptySet() else v } }
    return session.copy(board = board, notes = notes)
}

fun applySudokuToggleNote(session: SudokuSession, row: Int, col: Int, digit: Int): SudokuSession? {
    if (isGiven(session.puzzle, row, col) || session.finishedCells[row][col] || session.board[row][col] != null) return null

    val notes = session.notes.mapIndexed { r, line ->
        if (r != row) line else line.mapIndexed { c, cellNotes ->
            if (c != col) cellNotes else if (digit in cellNotes) cellNotes - digit else cellNotes + digit
        }
    }
    return session.copy(notes = notes)
}

data class SudokuValidationEffect(
    val correctRowIndexes: List<Int>,
    val correctColIndexes: List<Int>,
    val correctBoxIndexes: List<Int>,
    val incorrectRowIndexes: List<Int>,
    val incorrectColIndexes: List<Int>,
    val incorrectBoxIndexes: List<Int>,
)

data class SudokuFinalizeResult(val session: SudokuSession, val effect: SudokuValidationEffect)

private fun mergeFinishedCells(
    current: List<List<Boolean>>,
    board: SudokuGrid,
    givens: List<List<Int?>>,
    correctUnitKeys: List<SudokuUnitKey>,
): List<List<Boolean>> {
    val next = current.map { it.toMutableList() }.toMutableList()
    for (unitKey in correctUnitKeys) {
        val index = unitKey.substring(1).toInt()
        when (unitKey[0]) {
            'r' -> for (c in 0 until 9) if (givens[index][c] == null && board[index][c] != null) next[index][c] = true
            'c' -> for (r in 0 until 9) if (givens[r][index] == null && board[r][index] != null) next[r][index] = true
            else -> {
                val rowStart = (index / 3) * 3
                val colStart = (index % 3) * 3
                for (r in rowStart until rowStart + 3) for (c in colStart until colStart + 3) {
                    if (givens[r][c] == null && board[r][c] != null) next[r][c] = true
                }
            }
        }
    }
    return next
}

private fun groupUnitIndexes(unitKeys: List<SudokuUnitKey>): Triple<List<Int>, List<Int>, List<Int>> {
    val rows = mutableListOf<Int>(); val cols = mutableListOf<Int>(); val boxes = mutableListOf<Int>()
    for (key in unitKeys) {
        val index = key.substring(1).toInt()
        when (key[0]) {
            'r' -> rows.add(index)
            'c' -> cols.add(index)
            else -> boxes.add(index)
        }
    }
    return Triple(rows, cols, boxes)
}

fun applySudokuFinalizeValidation(session: SudokuSession, board: SudokuGrid, unitKeys: List<SudokuUnitKey>): SudokuFinalizeResult {
    val uniqueKeys = unitKeys.distinct()
    val completed = uniqueKeys.filter { getCompletedSudokuUnitState(board, session.puzzle.solution, it) != SudokuUnitState.INCOMPLETE }
    val incorrect = completed.filter { getCompletedSudokuUnitState(board, session.puzzle.solution, it) == SudokuUnitState.INCORRECT }
    val correct = completed.filter { getCompletedSudokuUnitState(board, session.puzzle.solution, it) == SudokuUnitState.CORRECT }
    val newPenaltyCount = incorrect.count { it !in session.penalizedUnitKeys }

    val (correctRows, correctCols, correctBoxes) = groupUnitIndexes(correct)
    val (incorrectRows, incorrectCols, incorrectBoxes) = groupUnitIndexes(incorrect)

    val updated = session.copy(
        board = board,
        accuracyDrops = session.accuracyDrops + newPenaltyCount,
        finishedCells = mergeFinishedCells(session.finishedCells, board, session.puzzle.givens, correct),
        penalizedUnitKeys = getMismatchedSudokuUnitKeys(board, session.puzzle.solution),
    )

    return SudokuFinalizeResult(
        session = updated,
        effect = SudokuValidationEffect(correctRows, correctCols, correctBoxes, incorrectRows, incorrectCols, incorrectBoxes),
    )
}

fun sudokuHasMeaningfulProgress(session: SudokuSession): Boolean {
    val boardChanged = session.board.indices.any { r -> session.board[r].indices.any { c -> session.board[r][c] != session.puzzle.givens[r][c] } }
    val hasNotes = session.notes.any { row -> row.any { it.isNotEmpty() } }
    return boardChanged || hasNotes || session.finishedCells.any { row -> row.any { it } } || session.accuracyDrops > 0 || session.penalizedUnitKeys.isNotEmpty()
}
