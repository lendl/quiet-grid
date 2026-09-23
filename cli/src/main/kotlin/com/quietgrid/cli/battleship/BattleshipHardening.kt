package com.quietgrid.cli.battleship

import com.quietgrid.engine.battleship.BattleshipSolveResult
import com.quietgrid.engine.battleship.analyzeBattleshipSolveResult
import com.quietgrid.engine.battleship.classifyBattleshipDifficulty
import com.quietgrid.engine.battleship.deriveClues
import com.quietgrid.engine.battleship.solveBattleship
import com.quietgrid.engine.core.Difficulty

data class BattleshipHardnessKey(
    val hardestTechniqueOrdinal: Int,
    val fleetLengthMatchCount: Int,
    val probingCount: Int,
) : Comparable<BattleshipHardnessKey> {
    override fun compareTo(other: BattleshipHardnessKey): Int {
        hardestTechniqueOrdinal.compareTo(other.hardestTechniqueOrdinal).let { if (it != 0) return it }
        fleetLengthMatchCount.compareTo(other.fleetLengthMatchCount).let { if (it != 0) return it }
        return probingCount.compareTo(other.probingCount)
    }
}

fun battleshipHardnessKeyOf(result: BattleshipSolveResult): BattleshipHardnessKey {
    val profile = analyzeBattleshipSolveResult(result)
    return BattleshipHardnessKey(profile.hardestTechnique.ordinal, profile.fleetLengthMatchCount, profile.probingCount)
}

private fun collectShip(board: Array<BooleanArray>, size: Int, visited: Array<BooleanArray>, row: Int, col: Int): List<Pair<Int, Int>> {
    val cells = mutableListOf(row to col)
    visited[row][col] = true
    var idx = 0
    while (idx < cells.size) {
        val (r, c) = cells[idx]
        idx++
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until size && nc in 0 until size && !visited[nr][nc] && board[nr][nc]) {
                visited[nr][nc] = true
                cells.add(nr to nc)
            }
        }
    }
    return cells
}

fun extractBattleshipShips(board: Array<BooleanArray>, size: Int): List<List<Pair<Int, Int>>> {
    val visited = Array(size) { BooleanArray(size) }
    val ships = mutableListOf<List<Pair<Int, Int>>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (board[row][col] && !visited[row][col]) ships.add(collectShip(board, size, visited, row, col))
        }
    }
    return ships
}

private fun canPlaceAt(board: Array<BooleanArray>, size: Int, cells: List<Pair<Int, Int>>): Boolean {
    for ((r, c) in cells) {
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until size && nc in 0 until size && board[nr][nc]) return false
        }
    }
    return true
}

fun mutateOneBattleshipShip(size: Int, board: Array<BooleanArray>): Array<BooleanArray>? {
    val ships = extractBattleshipShips(board, size)
    if (ships.isEmpty()) return null
    val target = ships.random()
    val length = target.size
    val mutated = Array(size) { row -> board[row].copyOf() }
    target.forEach { (r, c) -> mutated[r][c] = false }

    val candidates = mutableListOf<List<Pair<Int, Int>>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            val horizontal = (0 until length).map { row to (col + it) }
            if (horizontal.all { it.second < size } && canPlaceAt(mutated, size, horizontal)) candidates.add(horizontal)
            if (length > 1) {
                val vertical = (0 until length).map { (row + it) to col }
                if (vertical.all { it.first < size } && canPlaceAt(mutated, size, vertical)) candidates.add(vertical)
            }
        }
    }
    if (candidates.isEmpty()) return null
    candidates.random().forEach { (r, c) -> mutated[r][c] = true }
    return mutated
}

fun hardenTowardBattleshipDifficulty(
    size: Int,
    initialBoard: Array<BooleanArray>,
    fleet: List<Int>,
    targetDifficulty: Difficulty,
    maxStallMutations: Int = 2000,
): Array<BooleanArray> {
    var bestBoard = initialBoard
    val (initialRowClues, initialColClues) = deriveClues(initialBoard.map { it.toList() })
    var bestResult = solveBattleship(size, initialRowClues, initialColClues, fleet)
    var bestKey = battleshipHardnessKeyOf(bestResult)
    var stall = 0

    while (stall < maxStallMutations && classifyBattleshipDifficulty(bestResult) != targetDifficulty) {
        val mutated = mutateOneBattleshipShip(size, bestBoard)
        if (mutated == null) {
            stall++
            continue
        }
        val (rowClues, colClues) = deriveClues(mutated.map { it.toList() })
        val candidateResult = solveBattleship(size, rowClues, colClues, fleet)
        if (!candidateResult.solved) {
            stall++
            continue
        }
        val candidateDifficulty = classifyBattleshipDifficulty(candidateResult)
        if (candidateDifficulty != null && candidateDifficulty.ordinal > targetDifficulty.ordinal) {
            stall++
            continue
        }
        val candidateKey = battleshipHardnessKeyOf(candidateResult)
        if (candidateKey < bestKey) {
            stall++
            continue
        }
        val improved = candidateKey > bestKey
        bestBoard = mutated
        bestResult = candidateResult
        bestKey = candidateKey
        stall = if (improved) 0 else stall + 1
    }
    return bestBoard
}
