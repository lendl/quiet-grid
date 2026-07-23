package com.quietgrid.app.games.takuzu

import com.quietgrid.app.core.Difficulty
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SCORE = 10_000
private const val MIN_SCORE = 1_000
private const val ACCURACY_SCORE_COST = 500
private const val ACCURACY_DROP_STEP = 10

private val TIME_TO_ZERO_SECONDS = mapOf(
    Difficulty.EASY to 300,
    Difficulty.MEDIUM to 450,
    Difficulty.HARD to 600,
    Difficulty.EXPERT to 900,
)

fun takuzuScore(difficulty: Difficulty, timeSeconds: Int, accuracyDrops: Int): Int {
    val timeCap = TIME_TO_ZERO_SECONDS.getValue(difficulty)
    val timePenalty = (max(0, timeSeconds).toDouble() * MAX_SCORE) / timeCap
    val accuracyPenalty = accuracyDrops * ACCURACY_SCORE_COST
    return max(MIN_SCORE, (MAX_SCORE - timePenalty - accuracyPenalty).roundToInt())
}

fun takuzuAccuracyPct(accuracyDrops: Int): Int = max(0, 100 - accuracyDrops * ACCURACY_DROP_STEP)

fun createTakuzuSession(puzzle: TakuzuPuzzleEntry): TakuzuSession = TakuzuSession(
    puzzle = puzzle,
    board = decodePuzzleBoard(puzzle.solution, puzzle.mask, puzzle.size),
    solution = decodeSolution(puzzle.solution, puzzle.size),
    isGiven = decodeMask(puzzle.mask, puzzle.size),
    finishedCells = List(puzzle.size) { List(puzzle.size) { false } },
    accuracyDrops = 0,
    penalizedLineKeys = emptyList(),
)

/** Cycles a cell empty -> 0 -> 1 -> empty. Returns null if the tap has no effect (given or locked cell). */
fun applyTakuzuPressCell(session: TakuzuSession, row: Int, col: Int): TakuzuSession? {
    if (session.isGiven[row][col] || session.finishedCells[row][col]) return null

    val prev = session.board[row][col]
    val next = if (prev == null) 0 else if (prev == 0) 1 else null
    val board = session.board.mapIndexed { r, line ->
        if (r != row) line else line.mapIndexed { c, v -> if (c == col) next else v }
    }
    return session.copy(board = board)
}

data class TakuzuValidationEffect(
    val correctRowIndexes: List<Int>,
    val correctColIndexes: List<Int>,
    val incorrectRowIndexes: List<Int>,
    val incorrectColIndexes: List<Int>,
)

data class TakuzuFinalizeResult(val session: TakuzuSession, val effect: TakuzuValidationEffect)

private fun mergeFinishedCells(
    current: List<List<Boolean>>,
    board: TakuzuGrid,
    solution: TakuzuGrid,
    isGiven: List<List<Boolean>>,
    lineKeys: List<LineKey>,
): List<List<Boolean>> {
    val next = current.map { it.toMutableList() }.toMutableList()
    for (lineKey in lineKeys) {
        if (getCompletedLineStateForKey(board, solution, lineKey) != CompletedLineState.CORRECT) continue
        val index = lineKey.substring(1).toInt()
        if (lineKey.startsWith("r")) {
            for (c in board[index].indices) {
                if (!isGiven[index][c] && board[index][c] != null) next[index][c] = true
            }
        } else {
            for (r in board.indices) {
                if (!isGiven[r][index] && board[r][index] != null) next[r][index] = true
            }
        }
    }
    return next
}

/** Runs delayed line validation for the given (row/col) line keys touched by a press. */
fun applyTakuzuFinalizeValidation(session: TakuzuSession, board: TakuzuGrid, lineKeys: List<LineKey>): TakuzuFinalizeResult {
    val completedLineKeys = lineKeys.filter {
        getCompletedLineStateForKey(board, session.solution, it) != CompletedLineState.INCOMPLETE
    }
    val correctRowIndexes = completedLineKeys
        .filter { it.startsWith("r") && getCompletedLineStateForKey(board, session.solution, it) == CompletedLineState.CORRECT }
        .map { it.substring(1).toInt() }
    val correctColIndexes = completedLineKeys
        .filter { it.startsWith("c") && getCompletedLineStateForKey(board, session.solution, it) == CompletedLineState.CORRECT }
        .map { it.substring(1).toInt() }
    val incorrectRowIndexes = completedLineKeys
        .filter { it.startsWith("r") && getCompletedLineStateForKey(board, session.solution, it) == CompletedLineState.INCORRECT }
        .map { it.substring(1).toInt() }
    val incorrectColIndexes = completedLineKeys
        .filter { it.startsWith("c") && getCompletedLineStateForKey(board, session.solution, it) == CompletedLineState.INCORRECT }
        .map { it.substring(1).toInt() }

    val newPenalizedLineKeys = getMismatchedCompletedLines(board, session.solution)
    val newPenaltyCount = newPenalizedLineKeys.count { it !in session.penalizedLineKeys }

    val updated = session.copy(
        board = board,
        finishedCells = mergeFinishedCells(session.finishedCells, board, session.solution, session.isGiven, lineKeys),
        penalizedLineKeys = newPenalizedLineKeys,
        accuracyDrops = session.accuracyDrops + newPenaltyCount,
    )

    return TakuzuFinalizeResult(
        session = updated,
        effect = TakuzuValidationEffect(correctRowIndexes, correctColIndexes, incorrectRowIndexes, incorrectColIndexes),
    )
}

fun takuzuHasMeaningfulProgress(session: TakuzuSession): Boolean {
    val initialBoard = decodePuzzleBoard(session.puzzle.solution, session.puzzle.mask, session.puzzle.size)
    val boardChanged = session.board.indices.any { r -> session.board[r].indices.any { c -> session.board[r][c] != initialBoard[r][c] } }
    val hasFinished = session.finishedCells.any { row -> row.any { it } }
    return boardChanged || hasFinished || session.accuracyDrops > 0 || session.penalizedLineKeys.isNotEmpty()
}
