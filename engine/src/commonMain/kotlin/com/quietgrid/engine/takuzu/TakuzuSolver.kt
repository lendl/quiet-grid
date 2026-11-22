package com.quietgrid.engine.takuzu

fun countSolutions(puzzle: TakuzuGrid, maxCount: Int = 2): Int {
    val size = puzzle.size
    val grid: MutableList<MutableList<TakuzuCellValue>> = puzzle.map { it.toMutableList() }.toMutableList()
    var count = 0

    fun solve(pos: Int) {
        if (count >= maxCount) return
        if (pos == size * size) {
            if (hasUniqueLines(grid.map { row -> row.map { it!! } })) count += 1
            return
        }

        val row = pos / size
        val col = pos % size
        if (grid[row][col] != null) {
            solve(pos + 1)
            return
        }

        for (value in listOf(0, 1)) {
            grid[row][col] = value
            if (isLegalCell(grid, row, col, size)) solve(pos + 1)
            grid[row][col] = null
            if (count >= maxCount) return
        }
    }

    solve(0)
    return count
}

private const val MAX_BACKTRACKS = 10_000

fun generateSolvedGrid(size: Int): TakuzuGrid? {
    val grid: MutableList<MutableList<TakuzuCellValue>> = MutableList(size) { MutableList(size) { null } }
    var backtracks = 0

    fun solve(pos: Int): Boolean {
        if (pos == size * size) {
            return hasUniqueLines(grid.map { row -> row.map { it!! } })
        }

        val row = pos / size
        val col = pos % size
        val candidates = listOf(0, 1).shuffled()

        for (value in candidates) {
            grid[row][col] = value
            if (isLegalCell(grid, row, col, size)) {
                if (solve(pos + 1)) return true
            }
            grid[row][col] = null
            backtracks += 1
            if (backtracks > MAX_BACKTRACKS) return false
        }
        return false
    }

    return if (solve(0)) grid.map { it.toList() } else null
}
