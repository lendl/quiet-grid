package com.quietgrid.engine.battleship

private const val MAX_PLACEMENT_ATTEMPTS = 500

fun generateSolvedBoard(size: Int, fleet: List<Int>): List<List<Boolean>>? {
    repeat(MAX_PLACEMENT_ATTEMPTS) {
        val board = tryPlaceFleet(size, fleet)
        if (board != null) return board
    }
    return null
}

private fun tryPlaceFleet(size: Int, fleet: List<Int>): List<List<Boolean>>? {
    val ship = Array(size) { BooleanArray(size) }
    for (length in fleet.sortedDescending()) {
        if (!placeOneShip(size, ship, length)) return null
    }
    return ship.map { it.toList() }
}

private fun placeOneShip(size: Int, ship: Array<BooleanArray>, length: Int): Boolean {
    val positions = mutableListOf<List<Pair<Int, Int>>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            val horizontal = (0 until length).map { row to (col + it) }
            if (horizontal.all { it.second < size } && canPlaceShip(size, ship, horizontal)) positions.add(horizontal)
            if (length > 1) {
                val vertical = (0 until length).map { (row + it) to col }
                if (vertical.all { it.first < size } && canPlaceShip(size, ship, vertical)) positions.add(vertical)
            }
        }
    }
    if (positions.isEmpty()) return false
    positions.random().forEach { (r, c) -> ship[r][c] = true }
    return true
}

private fun canPlaceShip(size: Int, ship: Array<BooleanArray>, cells: List<Pair<Int, Int>>): Boolean {
    for ((r, c) in cells) {
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until size && nc in 0 until size && ship[nr][nc]) return false
        }
    }
    return true
}

fun deriveClues(board: List<List<Boolean>>): Pair<List<Int>, List<Int>> {
    val size = board.size
    val rowClues = (0 until size).map { row -> board[row].count { it } }
    val colClues = (0 until size).map { col -> (0 until size).count { row -> board[row][col] } }
    return rowClues to colClues
}
