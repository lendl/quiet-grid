package com.quietgrid.engine.takuzu

fun otherValue(value: Int): Int = if (value == 0) 1 else 0

fun countValue(line: List<TakuzuCellValue>, value: Int): Int = line.count { it == value }

fun countEmpties(line: List<TakuzuCellValue>): Int = line.count { it == null }

fun getColumn(grid: TakuzuGrid, col: Int): List<TakuzuCellValue> = grid.map { it[col] }

fun noThreeConsec(line: List<TakuzuCellValue>): Boolean {
    for (index in 0..line.size - 3) {
        val first = line[index]
        val second = line[index + 1]
        val third = line[index + 2]
        if (first != null && first == second && second == third) return false
    }
    return true
}

fun hasTriple(line: List<TakuzuCellValue>): Boolean = !noThreeConsec(line)

fun equalHalvesFeasible(line: List<TakuzuCellValue>, size: Int): Boolean {
    val half = size / 2
    return countValue(line, 0) <= half && countValue(line, 1) <= half
}

fun isLegalCell(grid: TakuzuGrid, row: Int, col: Int, size: Int): Boolean {
    val currentRow = grid[row]
    val currentCol = getColumn(grid, col)
    return noThreeConsec(currentRow) && noThreeConsec(currentCol) &&
        equalHalvesFeasible(currentRow, size) && equalHalvesFeasible(currentCol, size)
}

fun hasUniqueLines(grid: List<List<Int>>): Boolean {
    val size = grid.size
    val rowStrings = grid.map { it.joinToString("") }.toSet()
    if (rowStrings.size != size) return false

    val columnStrings = (0 until size).map { colIndex ->
        grid.joinToString("") { row -> row[colIndex].toString() }
    }.toSet()

    return columnStrings.size == size
}

fun lineWouldDuplicateCompleteLine(
    line: List<TakuzuCellValue>,
    completeLines: List<List<TakuzuCellValue>>,
): Boolean = completeLines.any { otherLine -> otherLine.indices.all { otherLine[it] == line[it] } }

/**
 * Checks whether placing [value] at ([row], [col]) keeps the board legal, without mutating
 * [board] permanently — places, checks, and restores to null.
 */
fun isCandidateLegal(board: MutableList<MutableList<TakuzuCellValue>>, row: Int, col: Int, value: Int): Boolean {
    val size = board.size
    board[row][col] = value

    if (!isLegalCell(board, row, col, size)) {
        board[row][col] = null
        return false
    }

    val rowValues = board[row]
    if (rowValues.all { it != null }) {
        val otherRows = board.filterIndexed { index, line -> index != row && line.all { it != null } }
        if (lineWouldDuplicateCompleteLine(rowValues, otherRows)) {
            board[row][col] = null
            return false
        }
    }

    val colValues = getColumn(board, col)
    if (colValues.all { it != null }) {
        val otherColumns = (0 until size).map { getColumn(board, it) }
            .filterIndexed { index, line -> index != col && line.all { it != null } }
        if (lineWouldDuplicateCompleteLine(colValues, otherColumns)) {
            board[row][col] = null
            return false
        }
    }

    board[row][col] = null
    return true
}
