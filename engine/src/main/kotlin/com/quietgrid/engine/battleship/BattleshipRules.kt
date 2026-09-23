package com.quietgrid.engine.battleship

private val ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun confirmedShipLengths(grid: List<List<BattleshipCell>>): List<Int> {
    val size = grid.size
    val visited = Array(size) { BooleanArray(size) }
    val lengths = mutableListOf<Int>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (grid[row][col] != BattleshipCell.SHIP || visited[row][col]) continue
            val cells = mutableListOf(row to col)
            visited[row][col] = true
            var idx = 0
            while (idx < cells.size) {
                val (r, c) = cells[idx]
                idx++
                for ((dr, dc) in ORTHOGONAL_DELTAS) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until size && nc in 0 until size && !visited[nr][nc] && grid[nr][nc] == BattleshipCell.SHIP) {
                        visited[nr][nc] = true
                        cells.add(nr to nc)
                    }
                }
            }
            val rows = cells.map { it.first }.toSet()
            val cols = cells.map { it.second }.toSet()
            val bounded = when {
                cells.size == 1 -> {
                    val (r0, c0) = cells[0]
                    ORTHOGONAL_DELTAS.all { (dr, dc) ->
                        val nr = r0 + dr
                        val nc = c0 + dc
                        nr !in 0 until size || nc !in 0 until size || grid[nr][nc] == BattleshipCell.WATER
                    }
                }
                rows.size == 1 -> {
                    val row0 = rows.first()
                    val minCol = cols.min()
                    val maxCol = cols.max()
                    (minCol == 0 || grid[row0][minCol - 1] == BattleshipCell.WATER) &&
                        (maxCol == size - 1 || grid[row0][maxCol + 1] == BattleshipCell.WATER)
                }
                else -> {
                    val col0 = cols.first()
                    val minRow = rows.min()
                    val maxRow = rows.max()
                    (minRow == 0 || grid[minRow - 1][col0] == BattleshipCell.WATER) &&
                        (maxRow == size - 1 || grid[maxRow + 1][col0] == BattleshipCell.WATER)
                }
            }
            if (bounded) lengths.add(cells.size)
        }
    }
    return lengths
}

fun remainingFleetCounts(grid: List<List<BattleshipCell>>, fleet: List<Int>): Map<Int, Int> {
    val initial = fleet.groupingBy { it }.eachCount()
    val confirmed = confirmedShipLengths(grid).groupingBy { it }.eachCount()
    return initial.mapValues { (length, count) -> count - (confirmed[length] ?: 0) }
}

fun isBattleshipSolved(grid: List<List<BattleshipCell>>, solutionShipCells: Set<Pair<Int, Int>>): Boolean {
    val size = grid.size
    for (row in 0 until size) {
        for (col in 0 until size) {
            val isShip = (row to col) in solutionShipCells
            val cell = grid[row][col]
            if (isShip && cell != BattleshipCell.SHIP) return false
            if (!isShip && cell != BattleshipCell.WATER) return false
        }
    }
    return true
}
