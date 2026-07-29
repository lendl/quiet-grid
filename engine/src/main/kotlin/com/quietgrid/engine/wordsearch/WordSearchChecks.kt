package com.quietgrid.engine.wordsearch

fun hasCoverageViolation(placements: List<WordPlacement>): Boolean {
    val sets = placements.map { it.positions.map { cell -> toGridKey(cell) }.toSet() }
    return sets.withIndex().any { (index, set) -> set.all { key -> sets.withIndex().any { (otherIndex, other) -> otherIndex != index && key in other } } }
}

private fun positionsMatch(a: List<WSCellRef>, b: List<WSCellRef>): Boolean {
    if (a.size != b.size) return false
    val forward = a.indices.all { a[it] == b[it] }
    if (forward) return true
    val n = b.size
    return a.indices.all { a[it] == b[n - 1 - it] }
}

/** Scans the finished [grid] in all 8 directions and confirms every entry in [words] appears ONLY at its intended position -- forward or reversed, nowhere else. */
fun hasDuplicateOccurrence(grid: List<List<String>>, words: List<Pair<String, List<WSCellRef>>>): Boolean {
    val rows = grid.size
    val cols = grid.firstOrNull()?.size ?: 0
    val allDirs = WordSearchDirection.entries

    val intendedByText = mutableMapOf<String, MutableList<List<WSCellRef>>>()
    fun addIntended(text: String, positions: List<WSCellRef>) { intendedByText.getOrPut(text) { mutableListOf() }.add(positions) }
    for ((word, positions) in words) {
        addIntended(word, positions)
        addIntended(word.reversed(), positions)
    }
    val wordLengths = words.map { it.first.length }.toSet()

    for (dir in allDirs) {
        val (dRow, dCol) = directionToDelta.getValue(dir)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val positions = mutableListOf<WSCellRef>()
                var sequence = StringBuilder()
                var r = row
                var c = col
                while (r in 0 until rows && c in 0 until cols) {
                    val cell = grid[r][c]
                    if (cell == "" || cell == "#") break
                    positions.add(WSCellRef(r, c))
                    sequence.append(cell)
                    if (sequence.length in wordLengths) {
                        val intendedList = intendedByText[sequence.toString()]
                        if (intendedList != null) {
                            val isIntended = intendedList.any { positionsMatch(positions, it) }
                            if (!isIntended) return true
                        }
                    }
                    r += dRow; c += dCol
                }
            }
        }
    }
    return false
}
