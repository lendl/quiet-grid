// engine/src/main/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuSolverState.kt
package com.quietgrid.engine.animaldoku

/** Mutable solve-time state for one AnimalDoku attempt. Not thread-safe; clone via [copy] for hypothesis branches. */
class AnimalDokuSolverState(val size: Int, val regionOf: List<List<Int>>) {
    val eliminated: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    val placedCol: IntArray = IntArray(size) { -1 }
    val rowSolved: BooleanArray = BooleanArray(size)
    val colSolved: BooleanArray = BooleanArray(size)
    val regionSolved: BooleanArray = BooleanArray(size)

    fun isCandidate(row: Int, col: Int): Boolean {
        if (eliminated[row][col]) return false
        if (rowSolved[row] || colSolved[col]) return false
        return !regionSolved[regionOf[row][col]]
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

    /** Places an animal at (row, col): solves its row/col/region and eliminates the 8 surrounding cells. */
    fun place(row: Int, col: Int) {
        check(placedCol[row] == -1) { "Row $row already has a placed animal." }
        placedCol[row] = col
        rowSolved[row] = true
        colSolved[col] = true
        regionSolved[regionOf[row][col]] = true
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = row + dr
            val nc = col + dc
            if (nr in 0 until size && nc in 0 until size) eliminated[nr][nc] = true
        }
    }

    fun isSolved(): Boolean = placedCol.all { it != -1 }

    fun copy(): AnimalDokuSolverState {
        val clone = AnimalDokuSolverState(size, regionOf)
        for (row in 0 until size) eliminated[row].copyInto(clone.eliminated[row])
        placedCol.copyInto(clone.placedCol)
        rowSolved.copyInto(clone.rowSolved)
        colSolved.copyInto(clone.colSolved)
        regionSolved.copyInto(clone.regionSolved)
        return clone
    }
}
