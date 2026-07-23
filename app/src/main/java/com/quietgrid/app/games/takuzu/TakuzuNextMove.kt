package com.quietgrid.app.games.takuzu

/**
 * Ports the RN app's Takuzu next-move hint techniques (src/games/takuzu/gameplay/analysis).
 * Recovery techniques run first (something on the board already breaks a rule); progress
 * techniques run next (a forced cell can be deduced). The deepest RN technique,
 * eliminate-impossible-combinations (a branch-and-prove search over line completions), is not
 * ported here — it is rare in practice, and puzzles that only have it available fall back to
 * the generic "paused" hint below instead of a specific technique.
 */

enum class TakuzuLineKind { ROW, COLUMN }

sealed interface TakuzuNextMoveHint {
    val evidenceCells: List<Pair<Int, Int>>
    val targetCells: List<Triple<Int, Int, Int>>
    val highlightRows: List<Int>
    val highlightCols: List<Int>

    data object Paused : TakuzuNextMoveHint {
        override val evidenceCells = emptyList<Pair<Int, Int>>()
        override val targetCells = emptyList<Triple<Int, Int, Int>>()
        override val highlightRows = emptyList<Int>()
        override val highlightCols = emptyList<Int>()
    }

    data class AvoidTriosRepair(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val repeatedValue: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint {
        override val targetCells = emptyList<Triple<Int, Int, Int>>()
    }

    data class CompleteLinesRepair(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val filledValue: Int,
        val filledCount: Int,
        val limit: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint {
        override val targetCells = emptyList<Triple<Int, Int, Int>>()
    }

    data class EliminateFilledLinesRepair(
        val lineKind: TakuzuLineKind,
        val firstLineIndex: Int,
        val secondLineIndex: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint {
        override val targetCells = emptyList<Triple<Int, Int, Int>>()
    }

    data class FindPairs(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val repeatedValue: Int,
        val targetValue: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val targetCells: List<Triple<Int, Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint

    data class AvoidTrios(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val repeatedValue: Int,
        val targetValue: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val targetCells: List<Triple<Int, Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint

    data class CompleteLines(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val filledValue: Int,
        val filledCount: Int,
        val targetValue: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val targetCells: List<Triple<Int, Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint

    data class EliminateFilledLines(
        val lineKind: TakuzuLineKind,
        val lineIndex: Int,
        val matchingLineIndex: Int,
        val targetValue: Int,
        override val evidenceCells: List<Pair<Int, Int>>,
        override val targetCells: List<Triple<Int, Int, Int>>,
        override val highlightRows: List<Int>,
        override val highlightCols: List<Int>,
    ) : TakuzuNextMoveHint
}

private fun otherValue(value: Int): Int = if (value == 0) 1 else 0

private fun getColumn(board: TakuzuGrid, col: Int): List<TakuzuCellValue> = board.map { it[col] }

private fun countValue(line: List<TakuzuCellValue>, value: Int): Int = line.count { it == value }

private fun getLine(board: TakuzuGrid, kind: TakuzuLineKind, index: Int): List<TakuzuCellValue> =
    if (kind == TakuzuLineKind.ROW) board[index] else getColumn(board, index)

private fun lineCells(kind: TakuzuLineKind, lineIndex: Int, indexes: List<Int>): List<Pair<Int, Int>> =
    indexes.map { index -> if (kind == TakuzuLineKind.ROW) lineIndex to index else index to lineIndex }

private fun lineTargets(kind: TakuzuLineKind, lineIndex: Int, indexes: List<Int>, value: Int): List<Triple<Int, Int, Int>> =
    indexes.map { index ->
        if (kind == TakuzuLineKind.ROW) Triple(lineIndex, index, value) else Triple(index, lineIndex, value)
    }

private fun highlightRowsFor(kind: TakuzuLineKind, lineIndex: Int): List<Int> = if (kind == TakuzuLineKind.ROW) listOf(lineIndex) else emptyList()
private fun highlightColsFor(kind: TakuzuLineKind, lineIndex: Int): List<Int> = if (kind == TakuzuLineKind.COLUMN) listOf(lineIndex) else emptyList()
private fun highlightRowsFor2(kind: TakuzuLineKind, a: Int, b: Int): List<Int> = if (kind == TakuzuLineKind.ROW) listOf(a, b) else emptyList()
private fun highlightColsFor2(kind: TakuzuLineKind, a: Int, b: Int): List<Int> = if (kind == TakuzuLineKind.COLUMN) listOf(a, b) else emptyList()

private fun findPairMoveInLine(line: List<TakuzuCellValue>): Pair<Int, Int>? {
    for (index in 0..line.size - 3) {
        val first = line[index]; val second = line[index + 1]; val third = line[index + 2]
        if (first != null && first == second && third == null) return (index + 2) to otherValue(first)
        if (first == null && second != null && second == third) return index to otherValue(second)
    }
    return null
}

private fun findAvoidTrioMoveInLine(line: List<TakuzuCellValue>): Pair<Int, Int>? {
    for (index in 0..line.size - 3) {
        val first = line[index]; val second = line[index + 1]; val third = line[index + 2]
        if (first != null && first == third && second == null) return (index + 1) to otherValue(first)
    }
    return null
}

private fun findTripleMismatch(board: TakuzuGrid): TakuzuNextMoveHint.AvoidTriosRepair? {
    val size = board.size
    for (row in 0 until size) {
        for (col in 0..size - 3) {
            val first = board[row][col]
            if (first != null && first == board[row][col + 1] && board[row][col + 1] == board[row][col + 2]) {
                return TakuzuNextMoveHint.AvoidTriosRepair(
                    lineKind = TakuzuLineKind.ROW,
                    lineIndex = row,
                    repeatedValue = first,
                    evidenceCells = lineCells(TakuzuLineKind.ROW, row, listOf(col, col + 1, col + 2)),
                    highlightRows = highlightRowsFor(TakuzuLineKind.ROW, row),
                    highlightCols = emptyList(),
                )
            }
        }
    }
    for (col in 0 until size) {
        for (row in 0..size - 3) {
            val first = board[row][col]
            if (first != null && first == board[row + 1][col] && board[row + 1][col] == board[row + 2][col]) {
                return TakuzuNextMoveHint.AvoidTriosRepair(
                    lineKind = TakuzuLineKind.COLUMN,
                    lineIndex = col,
                    repeatedValue = first,
                    evidenceCells = lineCells(TakuzuLineKind.COLUMN, col, listOf(row, row + 1, row + 2)),
                    highlightRows = emptyList(),
                    highlightCols = highlightColsFor(TakuzuLineKind.COLUMN, col),
                )
            }
        }
    }
    return null
}

private fun findBalanceMismatch(board: TakuzuGrid): TakuzuNextMoveHint.CompleteLinesRepair? {
    val size = board.size
    val limit = size / 2
    for (rowIndex in 0 until size) {
        val row = board[rowIndex]
        val zeroIndexes = row.indices.filter { row[it] == 0 }
        val oneIndexes = row.indices.filter { row[it] == 1 }
        if (zeroIndexes.size > limit) {
            return TakuzuNextMoveHint.CompleteLinesRepair(
                TakuzuLineKind.ROW, rowIndex, 0, zeroIndexes.size, limit,
                lineCells(TakuzuLineKind.ROW, rowIndex, zeroIndexes),
                highlightRowsFor(TakuzuLineKind.ROW, rowIndex), emptyList(),
            )
        }
        if (oneIndexes.size > limit) {
            return TakuzuNextMoveHint.CompleteLinesRepair(
                TakuzuLineKind.ROW, rowIndex, 1, oneIndexes.size, limit,
                lineCells(TakuzuLineKind.ROW, rowIndex, oneIndexes),
                highlightRowsFor(TakuzuLineKind.ROW, rowIndex), emptyList(),
            )
        }
    }
    for (colIndex in 0 until size) {
        val column = getColumn(board, colIndex)
        val zeroIndexes = column.indices.filter { column[it] == 0 }
        val oneIndexes = column.indices.filter { column[it] == 1 }
        if (zeroIndexes.size > limit) {
            return TakuzuNextMoveHint.CompleteLinesRepair(
                TakuzuLineKind.COLUMN, colIndex, 0, zeroIndexes.size, limit,
                lineCells(TakuzuLineKind.COLUMN, colIndex, zeroIndexes),
                emptyList(), highlightColsFor(TakuzuLineKind.COLUMN, colIndex),
            )
        }
        if (oneIndexes.size > limit) {
            return TakuzuNextMoveHint.CompleteLinesRepair(
                TakuzuLineKind.COLUMN, colIndex, 1, oneIndexes.size, limit,
                lineCells(TakuzuLineKind.COLUMN, colIndex, oneIndexes),
                emptyList(), highlightColsFor(TakuzuLineKind.COLUMN, colIndex),
            )
        }
    }
    return null
}

private fun findDuplicateMismatch(board: TakuzuGrid): TakuzuNextMoveHint.EliminateFilledLinesRepair? {
    val size = board.size
    val allIndexes = (0 until size).toList()
    val completedRows = board.indices.filter { r -> board[r].all { it != null } }
    for (i in completedRows.indices) {
        for (j in i + 1 until completedRows.size) {
            val a = completedRows[i]; val b = completedRows[j]
            if (board[a] == board[b]) {
                return TakuzuNextMoveHint.EliminateFilledLinesRepair(
                    TakuzuLineKind.ROW, a, b,
                    lineCells(TakuzuLineKind.ROW, a, allIndexes) + lineCells(TakuzuLineKind.ROW, b, allIndexes),
                    highlightRowsFor2(TakuzuLineKind.ROW, a, b), emptyList(),
                )
            }
        }
    }
    val completedCols = allIndexes.filter { c -> getColumn(board, c).all { it != null } }
    for (i in completedCols.indices) {
        for (j in i + 1 until completedCols.size) {
            val a = completedCols[i]; val b = completedCols[j]
            if (getColumn(board, a) == getColumn(board, b)) {
                return TakuzuNextMoveHint.EliminateFilledLinesRepair(
                    TakuzuLineKind.COLUMN, a, b,
                    lineCells(TakuzuLineKind.COLUMN, a, allIndexes) + lineCells(TakuzuLineKind.COLUMN, b, allIndexes),
                    emptyList(), highlightColsFor2(TakuzuLineKind.COLUMN, a, b),
                )
            }
        }
    }
    return null
}

private fun getTakuzuRecoveryHint(board: TakuzuGrid): TakuzuNextMoveHint? =
    findTripleMismatch(board) ?: findBalanceMismatch(board) ?: findDuplicateMismatch(board)

private fun findPairs(board: TakuzuGrid): TakuzuNextMoveHint.FindPairs? {
    val size = board.size
    for (row in 0 until size) {
        val move = findPairMoveInLine(board[row])
        if (move != null) return buildFindPairsHint(board, TakuzuLineKind.ROW, row, move.first, move.second)
    }
    for (col in 0 until size) {
        val move = findPairMoveInLine(getColumn(board, col))
        if (move != null) return buildFindPairsHint(board, TakuzuLineKind.COLUMN, col, move.first, move.second)
    }
    return null
}

private fun buildFindPairsHint(board: TakuzuGrid, kind: TakuzuLineKind, lineIndex: Int, targetIndex: Int, targetValue: Int): TakuzuNextMoveHint.FindPairs {
    val line = getLine(board, kind, lineIndex)
    val repeatedIndexes = if (targetIndex + 2 < line.size && line[targetIndex + 1] != null && line[targetIndex + 1] == line[targetIndex + 2]) {
        listOf(targetIndex + 1, targetIndex + 2)
    } else {
        listOf(targetIndex - 2, targetIndex - 1)
    }
    val repeatedValue = line[repeatedIndexes[0]]!!
    return TakuzuNextMoveHint.FindPairs(
        kind, lineIndex, repeatedValue, targetValue,
        lineCells(kind, lineIndex, repeatedIndexes),
        lineTargets(kind, lineIndex, listOf(targetIndex), targetValue),
        highlightRowsFor(kind, lineIndex), highlightColsFor(kind, lineIndex),
    )
}

private fun avoidTrios(board: TakuzuGrid): TakuzuNextMoveHint.AvoidTrios? {
    val size = board.size
    for (row in 0 until size) {
        val move = findAvoidTrioMoveInLine(board[row])
        if (move != null) return buildAvoidTriosHint(board, TakuzuLineKind.ROW, row, move.first, move.second)
    }
    for (col in 0 until size) {
        val move = findAvoidTrioMoveInLine(getColumn(board, col))
        if (move != null) return buildAvoidTriosHint(board, TakuzuLineKind.COLUMN, col, move.first, move.second)
    }
    return null
}

private fun buildAvoidTriosHint(board: TakuzuGrid, kind: TakuzuLineKind, lineIndex: Int, targetIndex: Int, targetValue: Int): TakuzuNextMoveHint.AvoidTrios {
    val line = getLine(board, kind, lineIndex)
    val repeatedIndexes = listOf(targetIndex - 1, targetIndex + 1)
    val repeatedValue = line[repeatedIndexes[0]]!!
    return TakuzuNextMoveHint.AvoidTrios(
        kind, lineIndex, repeatedValue, targetValue,
        lineCells(kind, lineIndex, repeatedIndexes),
        lineTargets(kind, lineIndex, listOf(targetIndex), targetValue),
        highlightRowsFor(kind, lineIndex), highlightColsFor(kind, lineIndex),
    )
}

private fun completeLines(board: TakuzuGrid): TakuzuNextMoveHint.CompleteLines? {
    val size = board.size
    val half = size / 2
    for (row in 0 until size) {
        val line = board[row]
        val zeroes = countValue(line, 0); val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val fillValue = if (zeroes == half) 1 else 0
            val col = line.indexOfFirst { it == null }
            if (col != -1) return buildCompleteLinesHint(board, TakuzuLineKind.ROW, row, col, fillValue)
        }
    }
    for (col in 0 until size) {
        val line = getColumn(board, col)
        val zeroes = countValue(line, 0); val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val fillValue = if (zeroes == half) 1 else 0
            val row = line.indexOfFirst { it == null }
            if (row != -1) return buildCompleteLinesHint(board, TakuzuLineKind.COLUMN, col, row, fillValue)
        }
    }
    return null
}

private fun buildCompleteLinesHint(board: TakuzuGrid, kind: TakuzuLineKind, lineIndex: Int, targetIndex: Int, targetValue: Int): TakuzuNextMoveHint.CompleteLines {
    val line = getLine(board, kind, lineIndex)
    val filledValue = otherValue(targetValue)
    val evidenceIndexes = line.indices.filter { line[it] == filledValue }
    return TakuzuNextMoveHint.CompleteLines(
        kind, lineIndex, filledValue, evidenceIndexes.size, targetValue,
        lineCells(kind, lineIndex, evidenceIndexes),
        lineTargets(kind, lineIndex, listOf(targetIndex), targetValue),
        highlightRowsFor(kind, lineIndex), highlightColsFor(kind, lineIndex),
    )
}

private fun eliminateFilledLines(board: TakuzuGrid): TakuzuNextMoveHint.EliminateFilledLines? =
    eliminateFilledLinesRow(board) ?: eliminateFilledLinesColumn(board)

private fun eliminateFilledLinesRow(board: TakuzuGrid): TakuzuNextMoveHint.EliminateFilledLines? {
    val size = board.size
    val completeRows = board.indices.filter { r -> board[r].all { it != null } }
    for (row in 0 until size) {
        val line = board[row]
        val emptyCols = line.indices.filter { line[it] == null }
        if (emptyCols.size != 2) continue
        for (complete in completeRows) {
            if (complete == row) continue
            val matches = line.indices.all { c -> line[c] == null || line[c] == board[complete][c] }
            if (!matches) continue
            val col = emptyCols[0]
            val value = otherValue(board[complete][col]!!)
            return buildEliminateFilledLinesHint(board, TakuzuLineKind.ROW, row, complete, col, value)
        }
    }
    return null
}

private fun eliminateFilledLinesColumn(board: TakuzuGrid): TakuzuNextMoveHint.EliminateFilledLines? {
    val size = board.size
    val completeCols = (0 until size).filter { c -> getColumn(board, c).all { it != null } }
    for (col in 0 until size) {
        val line = getColumn(board, col)
        val emptyRows = line.indices.filter { line[it] == null }
        if (emptyRows.size != 2) continue
        for (complete in completeCols) {
            if (complete == col) continue
            val completeLine = getColumn(board, complete)
            val matches = line.indices.all { r -> line[r] == null || line[r] == completeLine[r] }
            if (!matches) continue
            val row = emptyRows[0]
            val value = otherValue(completeLine[row]!!)
            return buildEliminateFilledLinesHint(board, TakuzuLineKind.COLUMN, col, complete, row, value)
        }
    }
    return null
}

private fun buildEliminateFilledLinesHint(
    board: TakuzuGrid,
    kind: TakuzuLineKind,
    lineIndex: Int,
    matchingLineIndex: Int,
    targetIndex: Int,
    targetValue: Int,
): TakuzuNextMoveHint.EliminateFilledLines {
    val size = board.size
    val allIndexes = (0 until size).toList()
    val line = getLine(board, kind, lineIndex)
    val filledIndexes = line.indices.filter { line[it] != null }
    return TakuzuNextMoveHint.EliminateFilledLines(
        kind, lineIndex, matchingLineIndex, targetValue,
        lineCells(kind, lineIndex, filledIndexes) + lineCells(kind, matchingLineIndex, allIndexes),
        lineTargets(kind, lineIndex, listOf(targetIndex), targetValue),
        if (kind == TakuzuLineKind.ROW) listOf(lineIndex, matchingLineIndex) else emptyList(),
        if (kind == TakuzuLineKind.COLUMN) listOf(lineIndex, matchingLineIndex) else emptyList(),
    )
}

private fun getTakuzuProgressHint(board: TakuzuGrid): TakuzuNextMoveHint? =
    findPairs(board) ?: avoidTrios(board) ?: completeLines(board) ?: eliminateFilledLines(board)

fun getTakuzuNextMoveHint(board: TakuzuGrid): TakuzuNextMoveHint =
    getTakuzuRecoveryHint(board) ?: getTakuzuProgressHint(board) ?: TakuzuNextMoveHint.Paused
