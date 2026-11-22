package com.quietgrid.engine.starbattle

fun countStarBattleSolutions(size: Int, k: Int, regionOf: List<List<Int>>, cap: Int = 2): Int {
    val rowStars = Array(size) { mutableListOf<Int>() }
    val colCount = IntArray(size)
    val regionCount = IntArray(size)
    var found = 0

    fun touchesPreviousRow(row: Int, col: Int): Boolean {
        if (row == 0) return false
        return rowStars[row - 1].any { kotlin.math.abs(it - col) <= 1 }
    }

    fun search(row: Int) {
        if (found >= cap) return
        if (row == size) {
            if (colCount.all { it == k } && regionCount.all { it == k }) found++
            return
        }
        fun pick(startIdx: Int, chosen: MutableList<Int>) {
            if (found >= cap) return
            if (chosen.size == k) {
                chosen.forEach { col -> colCount[col]++; regionCount[regionOf[row][col]]++ }
                rowStars[row] = chosen.toMutableList()
                search(row + 1)
                chosen.forEach { col -> colCount[col]--; regionCount[regionOf[row][col]]-- }
                return
            }
            for (col in startIdx until size) {
                if (found >= cap) return
                if (colCount[col] >= k) continue
                if (regionCount[regionOf[row][col]] >= k) continue
                if (chosen.any { kotlin.math.abs(it - col) <= 1 }) continue
                if (touchesPreviousRow(row, col)) continue
                chosen.add(col)
                pick(col + 1, chosen)
                chosen.removeAt(chosen.size - 1)
            }
        }
        pick(0, mutableListOf())
    }

    search(0)
    return found
}
