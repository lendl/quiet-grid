package com.quietgrid.engine.battleship

class BattleshipSolverState(
    val size: Int,
    val rowClues: IntArray,
    val colClues: IntArray,
    val fleet: List<Int>,
) {
    val grid: Array<Array<BattleshipCell>> = Array(size) { Array(size) { BattleshipCell.UNKNOWN } }

    fun shipCountInRow(row: Int) = (0 until size).count { grid[row][it] == BattleshipCell.SHIP }
    fun shipCountInCol(col: Int) = (0 until size).count { grid[it][col] == BattleshipCell.SHIP }
    fun unknownCountInRow(row: Int) = (0 until size).count { grid[row][it] == BattleshipCell.UNKNOWN }
    fun unknownCountInCol(col: Int) = (0 until size).count { grid[it][col] == BattleshipCell.UNKNOWN }
    fun unknownColsInRow(row: Int) = (0 until size).filter { grid[row][it] == BattleshipCell.UNKNOWN }
    fun unknownRowsInCol(col: Int) = (0 until size).filter { grid[it][col] == BattleshipCell.UNKNOWN }

    fun isSolved(): Boolean = grid.all { row -> row.none { it == BattleshipCell.UNKNOWN } }

    fun gridSnapshot(): List<List<BattleshipCell>> = grid.map { it.toList() }

    fun remainingFleetCounts(): Map<Int, Int> = remainingFleetCounts(gridSnapshot(), fleet)

    fun hasContradiction(): Boolean {
        for (row in 0 until size) {
            if (shipCountInRow(row) > rowClues[row]) return true
            if (shipCountInRow(row) + unknownCountInRow(row) < rowClues[row]) return true
        }
        for (col in 0 until size) {
            if (shipCountInCol(col) > colClues[col]) return true
            if (shipCountInCol(col) + unknownCountInCol(col) < colClues[col]) return true
        }
        if (remainingFleetCounts().values.any { it < 0 }) return true
        return false
    }

    fun copy(): BattleshipSolverState {
        val clone = BattleshipSolverState(size, rowClues, colClues, fleet)
        for (row in 0 until size) grid[row].copyInto(clone.grid[row])
        return clone
    }
}
