package com.quietgrid.engine.killersudoku

val SOLVED_GRID: IntArray = listOf(
    5, 3, 4, 6, 7, 8, 9, 1, 2,
    6, 7, 2, 1, 9, 5, 3, 4, 8,
    1, 9, 8, 3, 4, 2, 5, 6, 7,
    8, 5, 9, 7, 6, 1, 4, 2, 3,
    4, 2, 6, 8, 5, 3, 7, 9, 1,
    7, 1, 3, 9, 2, 4, 8, 5, 6,
    9, 6, 1, 5, 3, 7, 2, 8, 4,
    2, 8, 7, 4, 1, 9, 6, 3, 5,
    3, 4, 5, 2, 8, 6, 1, 7, 9,
).toIntArray()

fun layoutOf(groups: List<List<Int>>, solution: IntArray = SOLVED_GRID): KillerCageLayout {
    val grouped = groups.flatten().toSet()
    val singles = (0 until 81).filter { it !in grouped }.map { listOf(it) }
    return KillerCageLayout((groups + singles).map { cells -> KillerSudokuCage(cells.sumOf { solution[it] }, cells.sorted()) })
}

fun dominoLayout(solution: IntArray = SOLVED_GRID): KillerCageLayout {
    val groups = mutableListOf<List<Int>>()
    for (row in 0 until 9) {
        for (col in 0 until 8 step 2) groups += listOf(row * 9 + col, row * 9 + col + 1)
    }
    return layoutOf(groups, solution)
}
