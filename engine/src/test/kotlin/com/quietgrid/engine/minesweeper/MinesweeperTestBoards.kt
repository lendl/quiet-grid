package com.quietgrid.engine.minesweeper

import kotlin.random.Random

internal class TestLayout(val rows: Int, val cols: Int, val mines: BooleanArray) {
    val size get() = rows * cols
    val mineCount get() = mines.count { it }

    fun index(row: Int, col: Int) = row * cols + col

    fun neighbors(cell: Int): List<Int> {
        val r = cell / cols
        val c = cell % cols
        val result = mutableListOf<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) result.add(nr * cols + nc)
        }
        return result
    }

    fun number(cell: Int) = neighbors(cell).count { mines[it] }

    fun hiddenView() = IntArray(size) { MINESWEEPER_HIDDEN }

    fun reveal(view: IntArray, start: Int) {
        val stack = ArrayDeque<Int>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            if (view[cell] != MINESWEEPER_HIDDEN || mines[cell]) continue
            view[cell] = number(cell)
            if (view[cell] == 0) stack.addAll(neighbors(cell))
        }
    }

    fun isCleared(view: IntArray) = (0 until size).all { mines[it] || view[it] != MINESWEEPER_HIDDEN }
}

internal fun layout(vararg lines: String): TestLayout {
    val cols = lines[0].length
    return TestLayout(lines.size, cols, BooleanArray(lines.size * cols) { lines[it / cols][it % cols] == '*' })
}

internal fun randomLayout(rows: Int, cols: Int, mineCount: Int, start: Int, random: Random): TestLayout {
    val shell = TestLayout(rows, cols, BooleanArray(rows * cols))
    val protectedCells = shell.neighbors(start).toSet() + start
    val candidates = (0 until rows * cols).filter { it !in protectedCells }.shuffled(random).take(mineCount).toSet()
    return TestLayout(rows, cols, BooleanArray(rows * cols) { it in candidates })
}
