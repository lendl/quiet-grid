package com.quietgrid.cli.battleship

import com.quietgrid.engine.battleship.BattleshipCell
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import com.quietgrid.engine.battleship.BattleshipSolveResult
import com.quietgrid.engine.battleship.classifyBattleshipDifficulty
import com.quietgrid.engine.battleship.countBattleshipSolutions
import com.quietgrid.engine.battleship.deriveClues
import com.quietgrid.engine.battleship.encodeBattleshipGivens
import com.quietgrid.engine.battleship.encodeBattleshipSolution
import com.quietgrid.engine.battleship.generateSolvedBoard
import com.quietgrid.engine.battleship.solveBattleship
import com.quietgrid.engine.core.Difficulty

private fun revealGivensUntilSolvable(
    size: Int,
    board: Array<BooleanArray>,
    rowClues: List<Int>,
    colClues: List<Int>,
    fleet: List<Int>,
): Pair<List<Triple<Int, Int, BattleshipCell>>, BattleshipSolveResult>? {
    var result = solveBattleship(size, rowClues, colClues, fleet)
    if (result.solved) return emptyList<Triple<Int, Int, BattleshipCell>>() to result

    val givens = mutableListOf<Triple<Int, Int, BattleshipCell>>()
    var guard = 0
    while (!result.solved && guard < size * size) {
        guard++
        val unknownCells = result.board.indices.flatMap { row ->
            result.board[row].indices.mapNotNull { col ->
                if (result.board[row][col] == BattleshipCell.UNKNOWN) row to col else null
            }
        }
        if (unknownCells.isEmpty()) break
        val unknownShipCells = unknownCells.filter { (r, c) -> board[r][c] }
        val (row, col) = (unknownShipCells.ifEmpty { unknownCells }).random()
        val cell = if (board[row][col]) BattleshipCell.SHIP else BattleshipCell.WATER
        givens.add(Triple(row, col, cell))
        result = solveBattleship(size, rowClues, colClues, fleet, givens)
    }
    if (!result.solved) return null
    return givens to result
}

fun generateBattleshipPuzzleForTier(size: Int, fleet: List<Int>, targetDifficulty: Difficulty, idPrefix: String): BattleshipPuzzleEntry? {
    val solvedBoard = generateSolvedBoard(size, fleet) ?: return null
    var board = Array(size) { row -> BooleanArray(size) { col -> solvedBoard[row][col] } }

    if (targetDifficulty == Difficulty.HARD || targetDifficulty == Difficulty.EXPERT) {
        board = hardenTowardBattleshipDifficulty(size, board, fleet, targetDifficulty)
    }

    val (rowClues, colClues) = deriveClues(board.map { it.toList() })
    val (givens, result) = revealGivensUntilSolvable(size, board, rowClues, colClues, fleet) ?: return null

    if (classifyBattleshipDifficulty(result) != targetDifficulty) return null

    val givenTriples = givens.map { (row, col, cell) -> Triple(row, col, cell == BattleshipCell.SHIP) }
    if (countBattleshipSolutions(size, rowClues, colClues, fleet, cap = 2, givens = givenTriples) != 1) return null

    val solutionString = encodeBattleshipSolution(board.map { it.toList() })
    val givensString = encodeBattleshipGivens(givens, size)
    return BattleshipPuzzleEntry(
        id = "$idPrefix-${solutionString.hashCode()}",
        size = size,
        difficulty = targetDifficulty.key,
        rowClues = rowClues,
        colClues = colClues,
        fleet = fleet,
        solution = solutionString,
        givens = givensString,
    )
}
