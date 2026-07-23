package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.core.Difficulty
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SCORE = 10_000
private const val MIN_SCORE = 1_000

private val TIME_TO_ZERO_SECONDS = mapOf(
    Difficulty.EASY to 300,
    Difficulty.MEDIUM to 450,
    Difficulty.HARD to 600,
    Difficulty.EXPERT to 900,
)

fun createMinesweeperSession(difficulty: Difficulty): MinesweeperSession {
    val puzzle = createMinesweeperPuzzle(difficulty)
    return MinesweeperSession(puzzle = puzzle, board = createMinesweeperBoard(puzzle))
}

fun minesweeperScore(difficulty: Difficulty, timeSeconds: Int): Int {
    val timeCap = TIME_TO_ZERO_SECONDS.getValue(difficulty)
    val timePenalty = (max(0, timeSeconds).toDouble() * MAX_SCORE) / timeCap
    return max(MIN_SCORE, (MAX_SCORE - timePenalty).roundToInt())
}

fun minesweeperHasMeaningfulProgress(session: MinesweeperSession): Boolean =
    session.board.generated || session.board.cells.any { row -> row.any { it.state != MinesweeperCellState.HIDDEN } }
