package com.quietgrid.cli.starbattle

fun generateStarBattleSolution(size: Int, k: Int, maxAttempts: Int = 2000): List<List<Int>>? {
    repeat(maxAttempts) {
        val result = tryBuildStarBattleSolution(size, k)
        if (result != null) return result
    }
    return null
}

private fun tryBuildStarBattleSolution(size: Int, k: Int): List<List<Int>>? {
    val colCount = IntArray(size)
    val rows = mutableListOf<List<Int>>()
    for (row in 0 until size) {
        val prevRowCols = rows.lastOrNull() ?: emptyList()
        val available = (0 until size).filter { col -> colCount[col] < k }
        val chosen = chooseNonTouchingColumns(available, prevRowCols, k) ?: return null
        chosen.forEach { colCount[it]++ }
        rows.add(chosen.sorted())
    }
    return if (colCount.all { it == k }) rows else null
}

private fun chooseNonTouchingColumns(available: List<Int>, prevRowCols: List<Int>, k: Int): List<Int>? {
    val usable = available.filter { col -> prevRowCols.none { prev -> kotlin.math.abs(col - prev) <= 1 } }
    if (usable.size < k) return null
    val shuffled = usable.shuffled()
    val chosen = mutableListOf<Int>()
    for (col in shuffled) {
        if (chosen.size == k) break
        if (chosen.none { kotlin.math.abs(it - col) <= 1 }) chosen.add(col)
    }
    return if (chosen.size == k) chosen else null
}
