package com.quietgrid.engine.starbattle

class StarBattleSolverState(val size: Int, val k: Int, val regionOf: List<List<Int>>) {
    val eliminated: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    val placedCols: Array<MutableSet<Int>> = Array(size) { mutableSetOf() }
    val rowCount: IntArray = IntArray(size)
    val colCount: IntArray = IntArray(size)
    val regionCount: IntArray = IntArray(size)

    fun rowRemaining(row: Int): Int = k - rowCount[row]
    fun colRemaining(col: Int): Int = k - colCount[col]
    fun regionRemaining(region: Int): Int = k - regionCount[region]

    fun isCandidate(row: Int, col: Int): Boolean {
        if (eliminated[row][col]) return false
        if (col in placedCols[row]) return false
        if (rowRemaining(row) <= 0 || colRemaining(col) <= 0) return false
        return regionRemaining(regionOf[row][col]) > 0
    }

    fun candidatesInRegion(region: Int): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) for (col in 0 until size) {
            if (regionOf[row][col] == region && isCandidate(row, col)) cells.add(row to col)
        }
        return cells
    }

    fun candidatesInRow(row: Int): List<Pair<Int, Int>> =
        (0 until size).filter { col -> isCandidate(row, col) }.map { col -> row to col }

    fun candidatesInCol(col: Int): List<Pair<Int, Int>> =
        (0 until size).filter { row -> isCandidate(row, col) }.map { row -> row to col }

    fun place(row: Int, col: Int) {
        check(col !in placedCols[row]) { "Row $row already has a star at column $col." }
        placedCols[row].add(col)
        rowCount[row]++
        colCount[col]++
        regionCount[regionOf[row][col]]++
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = row + dr
            val nc = col + dc
            if (nr in 0 until size && nc in 0 until size) eliminated[nr][nc] = true
        }
    }

    fun isSolved(): Boolean = rowCount.all { it == k }

    fun copy(): StarBattleSolverState {
        val clone = StarBattleSolverState(size, k, regionOf)
        for (row in 0 until size) {
            eliminated[row].copyInto(clone.eliminated[row])
            clone.placedCols[row].addAll(placedCols[row])
        }
        rowCount.copyInto(clone.rowCount)
        colCount.copyInto(clone.colCount)
        regionCount.copyInto(clone.regionCount)
        return clone
    }
}
