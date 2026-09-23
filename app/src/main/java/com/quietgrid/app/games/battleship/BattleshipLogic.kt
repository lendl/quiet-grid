package com.quietgrid.app.games.battleship

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.battleship.BattleshipBoard
import com.quietgrid.engine.battleship.BattleshipCell
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import com.quietgrid.engine.battleship.decodeBattleshipGivens
import com.quietgrid.engine.battleship.decodeBattleshipSolutionCells
import com.quietgrid.engine.battleship.isBattleshipSolved
import kotlin.math.max

private const val MAX_SCORE = 10_000
private const val MIN_SCORE = 1_000
private val TIME_TO_ZERO_SECONDS = mapOf(
    Difficulty.EASY to 300,
    Difficulty.MEDIUM to 450,
    Difficulty.HARD to 600,
    Difficulty.EXPERT to 900,
)

fun createBattleshipSession(puzzle: BattleshipPuzzleEntry): BattleshipSession {
    val givens = decodeBattleshipGivens(puzzle.givens, puzzle.size)
    val givenByCell = givens.associate { (row, col, cell) -> (row to col) to cell }
    val board = List(puzzle.size) { row ->
        List(puzzle.size) { col -> givenByCell[row to col] ?: BattleshipCell.UNKNOWN }
    }
    return BattleshipSession(
        puzzle = puzzle,
        board = board,
        solutionShipCells = decodeBattleshipSolutionCells(puzzle.solution, puzzle.size),
        givenCells = givenByCell.keys,
    )
}

fun encodeBattleshipBoard(board: BattleshipBoard): String = board.joinToString("") { row ->
    row.joinToString("") { cell ->
        when (cell) {
            BattleshipCell.UNKNOWN -> "U"
            BattleshipCell.WATER -> "W"
            BattleshipCell.SHIP -> "S"
        }
    }
}

fun decodeBattleshipBoard(encoded: String, size: Int): BattleshipBoard = List(size) { row ->
    List(size) { col ->
        when (encoded[row * size + col]) {
            'W' -> BattleshipCell.WATER
            'S' -> BattleshipCell.SHIP
            else -> BattleshipCell.UNKNOWN
        }
    }
}

fun applyBattleshipPressCell(session: BattleshipSession, row: Int, col: Int): BattleshipSession {
    if ((row to col) in session.givenCells) return session
    val nextCell = when (session.board[row][col]) {
        BattleshipCell.UNKNOWN -> BattleshipCell.WATER
        BattleshipCell.WATER -> BattleshipCell.SHIP
        BattleshipCell.SHIP -> BattleshipCell.UNKNOWN
    }
    val nextBoard = session.board.mapIndexed { r, rowCells ->
        if (r != row) rowCells else rowCells.mapIndexed { c, cell -> if (c == col) nextCell else cell }
    }
    return session.copy(board = nextBoard)
}

fun battleshipIsSolved(session: BattleshipSession): Boolean = isBattleshipSolved(session.board, session.solutionShipCells)

fun battleshipHasMeaningfulProgress(session: BattleshipSession): Boolean =
    session.board.indices.any { row ->
        session.board[row].indices.any { col -> (row to col) !in session.givenCells && session.board[row][col] != BattleshipCell.UNKNOWN }
    }

fun battleshipScore(difficulty: Difficulty, timeSeconds: Int): Int {
    val timeToZero = TIME_TO_ZERO_SECONDS.getValue(difficulty)
    val timeFraction = max(0.0, 1.0 - timeSeconds.toDouble() / timeToZero)
    return (MIN_SCORE + (MAX_SCORE - MIN_SCORE) * timeFraction).toInt()
}
