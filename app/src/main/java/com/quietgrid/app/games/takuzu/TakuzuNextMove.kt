package com.quietgrid.app.games.takuzu

import com.quietgrid.engine.takuzu.TakuzuCellValue
import com.quietgrid.engine.takuzu.TakuzuGrid
import com.quietgrid.engine.takuzu.countValue
import com.quietgrid.engine.takuzu.findAvoidTrioMoveInLine
import com.quietgrid.engine.takuzu.findCompleteLineMove
import com.quietgrid.engine.takuzu.findEliminateFilledLinesColumnMove
import com.quietgrid.engine.takuzu.findEliminateFilledLinesRowMove
import com.quietgrid.engine.takuzu.findPairMoveInLine
import com.quietgrid.engine.takuzu.getColumn
import com.quietgrid.engine.takuzu.otherValue

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
    for (row in 0 until size) {
        val move = findCompleteLineMove(board[row], size) ?: continue
        return buildCompleteLinesHint(board, TakuzuLineKind.ROW, row, move.first, move.second)
    }
    for (col in 0 until size) {
        val move = findCompleteLineMove(getColumn(board, col), size) ?: continue
        return buildCompleteLinesHint(board, TakuzuLineKind.COLUMN, col, move.first, move.second)
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

private fun eliminateFilledLines(board: TakuzuGrid): TakuzuNextMoveHint.EliminateFilledLines? {
    val rowMove = findEliminateFilledLinesRowMove(board)
    if (rowMove != null) {
        return buildEliminateFilledLinesHint(board, TakuzuLineKind.ROW, rowMove.row, rowMove.matchingLineIndex!!, rowMove.col, rowMove.value)
    }
    val colMove = findEliminateFilledLinesColumnMove(board)
    if (colMove != null) {
        return buildEliminateFilledLinesHint(board, TakuzuLineKind.COLUMN, colMove.col, colMove.matchingLineIndex!!, colMove.row, colMove.value)
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
