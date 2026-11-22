package com.quietgrid.engine.minesweeper

fun isMinesweeperSolvableWithoutGuessing(rows: Int, cols: Int, mines: BooleanArray, start: Int): Boolean {
    if (mines[start]) return false
    val state = MinesweeperSolverState(rows, cols, mines.count { it })
    val numbers = IntArray(state.size) { cell -> state.neighbors[cell].count { mines[it] } }
    val stack = ArrayDeque<Int>()

    fun reveal(cell: Int) {
        stack.add(cell)
        while (stack.isNotEmpty()) {
            val next = stack.removeLast()
            if (!state.isUnknown(next) || mines[next]) continue
            state.open(next, numbers[next])
            if (numbers[next] == 0) state.neighbors[next].forEach(stack::add)
        }
    }

    reveal(start)
    val safeTotal = state.size - state.totalMines
    while (state.openCount < safeTotal) {
        val deductions = nextMinesweeperDeductions(state)
        if (deductions.isEmpty()) return false
        for (deduction in deductions) {
            if (deduction.safe.any { mines[it] } || deduction.mines.any { !mines[it] }) return false
            deduction.mines.forEach(state::markMine)
            deduction.safe.forEach(::reveal)
        }
    }
    return true
}

fun findMinesweeperSafeDeduction(rows: Int, cols: Int, totalMines: Int, view: IntArray): MinesweeperDeduction? {
    val state = MinesweeperSolverState(rows, cols, totalMines)
    for (cell in view.indices) if (view[cell] != MINESWEEPER_HIDDEN) state.open(cell, view[cell])
    while (true) {
        val deductions = nextMinesweeperDeductions(state)
        if (deductions.isEmpty()) return null
        deductions.firstOrNull { it.safe.isNotEmpty() }?.let { return it }
        for (deduction in deductions) deduction.mines.forEach(state::markMine)
    }
}
