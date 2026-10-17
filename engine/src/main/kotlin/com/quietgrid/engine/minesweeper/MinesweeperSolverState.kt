package com.quietgrid.engine.minesweeper

internal class MinesweeperSolverState(val rows: Int, val cols: Int, val totalMines: Int) {
    val size = rows * cols
    val neighbors: Array<IntArray> = Array(size) { cellsAround(it, 1) }
    val nearby: Array<IntArray> = Array(size) { cellsAround(it, 2) }
    private val status = IntArray(size) { UNKNOWN }
    private val numbers = IntArray(size)

    var openCount = 0
        private set
    var mineCount = 0
        private set
    val unknownCount get() = size - openCount - mineCount

    fun isUnknown(cell: Int) = status[cell] == UNKNOWN
    fun isOpen(cell: Int) = status[cell] == OPEN

    fun open(cell: Int, number: Int) {
        if (status[cell] != UNKNOWN) return
        status[cell] = OPEN
        numbers[cell] = number
        openCount++
    }

    fun markMine(cell: Int) {
        if (status[cell] != UNKNOWN) return
        status[cell] = MINE
        mineCount++
    }

    fun unknownNeighbors(cell: Int): List<Int> = neighbors[cell].filter { status[it] == UNKNOWN }

    fun remainingMines(clue: Int): Int = numbers[clue] - neighbors[clue].count { status[it] == MINE }

    private fun cellsAround(cell: Int, radius: Int): IntArray {
        val row = cell / cols
        val col = cell % cols
        val result = ArrayList<Int>()
        for (r in row - radius..row + radius) for (c in col - radius..col + radius) {
            if (r !in 0 until rows || c !in 0 until cols || (r == row && c == col)) continue
            result.add(r * cols + c)
        }
        return result.toIntArray()
    }

    private companion object {
        const val UNKNOWN = 0
        const val OPEN = 1
        const val MINE = 2
    }
}
