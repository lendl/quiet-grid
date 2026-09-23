package com.quietgrid.engine.battleship

private fun placementsForLength(size: Int, length: Int): List<List<Pair<Int, Int>>> {
    val result = mutableListOf<List<Pair<Int, Int>>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            val horizontal = (0 until length).map { row to (col + it) }
            if (horizontal.all { it.second < size }) result.add(horizontal)
        }
    }
    if (length > 1) {
        for (row in 0 until size) {
            for (col in 0 until size) {
                val vertical = (0 until length).map { (row + it) to col }
                if (vertical.all { it.first < size }) result.add(vertical)
            }
        }
    }
    return result
}

fun countBattleshipSolutions(
    size: Int,
    rowClues: List<Int>,
    colClues: List<Int>,
    fleet: List<Int>,
    cap: Int = 2,
    givens: List<Triple<Int, Int, Boolean>> = emptyList(),
): Int {
    val ship = Array(size) { BooleanArray(size) }
    val rowCount = IntArray(size)
    val colCount = IntArray(size)
    val lengthsDescending = fleet.sortedDescending()
    val placementsByLength = lengthsDescending.toSet().associateWith { placementsForLength(size, it) }
    var found = 0

    fun canPlace(cells: List<Pair<Int, Int>>): Boolean {
        for ((r, c) in cells) {
            if (rowCount[r] + 1 > rowClues[r]) return false
            if (colCount[c] + 1 > colClues[c]) return false
            for (dr in -1..1) for (dc in -1..1) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until size && nc in 0 until size && ship[nr][nc]) return false
            }
        }
        return true
    }

    fun place(cells: List<Pair<Int, Int>>) {
        cells.forEach { (r, c) -> ship[r][c] = true; rowCount[r]++; colCount[c]++ }
    }

    fun unplace(cells: List<Pair<Int, Int>>) {
        cells.forEach { (r, c) -> ship[r][c] = false; rowCount[r]--; colCount[c]-- }
    }

    fun search(shipIndex: Int, sameLengthStartIndex: Int) {
        if (found >= cap) return
        if (shipIndex == lengthsDescending.size) {
            val satisfiesClues = rowCount.indices.all { rowCount[it] == rowClues[it] } && colCount.indices.all { colCount[it] == colClues[it] }
            val satisfiesGivens = givens.all { (r, c, isShip) -> ship[r][c] == isShip }
            if (satisfiesClues && satisfiesGivens) found++
            return
        }
        val length = lengthsDescending[shipIndex]
        val placements = placementsByLength.getValue(length)
        val startIndex = if (shipIndex > 0 && lengthsDescending[shipIndex - 1] == length) sameLengthStartIndex else 0
        for (i in startIndex until placements.size) {
            if (found >= cap) return
            val cells = placements[i]
            if (canPlace(cells)) {
                place(cells)
                search(shipIndex + 1, i + 1)
                unplace(cells)
            }
        }
    }

    search(0, 0)
    return found
}
