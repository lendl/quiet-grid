package com.quietgrid.engine.takuzu

enum class TakuzuContradictionKind { TRIPLE, BALANCE, DUPLICATE_LINE }

data class TakuzuContradiction(val kind: TakuzuContradictionKind, val isRow: Boolean, val lineIndex: Int)

data class TakuzuHumanProofStep(val row: Int, val col: Int, val value: Int)

sealed class TakuzuHumanProofResult {
    abstract val steps: List<TakuzuHumanProofStep>

    data class Contradiction(
        override val steps: List<TakuzuHumanProofStep>,
        val contradiction: TakuzuContradiction,
    ) : TakuzuHumanProofResult()

    data class Stalled(override val steps: List<TakuzuHumanProofStep>) : TakuzuHumanProofResult()
}

private fun lineWouldDuplicateCompleteLineMutable(
    line: List<Int?>,
    completeLines: List<List<Int?>>,
): Boolean = completeLines.any { other -> other.indices.all { other[it] == line[it] } }

private fun detectTriple(board: List<MutableList<TakuzuCellValue>>): TakuzuContradiction? {
    val size = board.size
    for (row in 0 until size) {
        for (col in 0..size - 3) {
            val first = board[row][col]
            if (first != null && first == board[row][col + 1] && board[row][col + 1] == board[row][col + 2]) {
                return TakuzuContradiction(TakuzuContradictionKind.TRIPLE, isRow = true, lineIndex = row)
            }
        }
    }
    for (col in 0 until size) {
        for (row in 0..size - 3) {
            val first = board[row][col]
            if (first != null && first == board[row + 1][col] && board[row + 1][col] == board[row + 2][col]) {
                return TakuzuContradiction(TakuzuContradictionKind.TRIPLE, isRow = false, lineIndex = col)
            }
        }
    }
    return null
}

private fun detectBalanceOverflow(board: List<MutableList<TakuzuCellValue>>): TakuzuContradiction? {
    val size = board.size
    val half = size / 2
    for (row in 0 until size) {
        val line = board[row]
        if (countValue(line, 0) > half || countValue(line, 1) > half) {
            return TakuzuContradiction(TakuzuContradictionKind.BALANCE, isRow = true, lineIndex = row)
        }
    }
    for (col in 0 until size) {
        val line = getColumn(board, col)
        if (countValue(line, 0) > half || countValue(line, 1) > half) {
            return TakuzuContradiction(TakuzuContradictionKind.BALANCE, isRow = false, lineIndex = col)
        }
    }
    return null
}

private fun detectDuplicateCompletedLine(board: List<MutableList<TakuzuCellValue>>): TakuzuContradiction? {
    val size = board.size
    val completeRows = board.indices.filter { r -> board[r].all { it != null } }
    for (i in completeRows.indices) {
        val row = completeRows[i]
        val otherRows = completeRows.filter { it != row }.map { board[it] }
        if (lineWouldDuplicateCompleteLineMutable(board[row], otherRows)) {
            return TakuzuContradiction(TakuzuContradictionKind.DUPLICATE_LINE, isRow = true, lineIndex = row)
        }
    }
    val completeCols = (0 until size).filter { c -> getColumn(board, c).all { it != null } }
    for (i in completeCols.indices) {
        val col = completeCols[i]
        val colLine = getColumn(board, col)
        val otherCols = completeCols.filter { it != col }.map { getColumn(board, it) }
        if (lineWouldDuplicateCompleteLineMutable(colLine, otherCols)) {
            return TakuzuContradiction(TakuzuContradictionKind.DUPLICATE_LINE, isRow = false, lineIndex = col)
        }
    }
    return null
}

private fun findHumanProofStep(board: List<MutableList<TakuzuCellValue>>): TakuzuHumanProofStep? {
    val size = board.size

    for (row in 0 until size) {
        val move = findPairMoveInLine(board[row]) ?: continue
        return TakuzuHumanProofStep(row, move.first, move.second)
    }
    for (col in 0 until size) {
        val move = findPairMoveInLine(getColumn(board, col)) ?: continue
        return TakuzuHumanProofStep(move.first, col, move.second)
    }
    for (row in 0 until size) {
        val move = findAvoidTrioMoveInLine(board[row]) ?: continue
        return TakuzuHumanProofStep(row, move.first, move.second)
    }
    for (col in 0 until size) {
        val move = findAvoidTrioMoveInLine(getColumn(board, col)) ?: continue
        return TakuzuHumanProofStep(move.first, col, move.second)
    }

    val half = size / 2
    for (row in 0 until size) {
        val line = board[row]
        val zeroes = countValue(line, 0)
        val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val col = line.indexOfFirst { it == null }
            if (col != -1) return TakuzuHumanProofStep(row, col, if (zeroes == half) 1 else 0)
        }
    }
    for (col in 0 until size) {
        val line = getColumn(board, col)
        val zeroes = countValue(line, 0)
        val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val row = line.indexOfFirst { it == null }
            if (row != -1) return TakuzuHumanProofStep(row, col, if (zeroes == half) 1 else 0)
        }
    }

    val eliminateRow = findEliminateFilledLinesMove(board.map { it.toList() })
    if (eliminateRow != null) return TakuzuHumanProofStep(eliminateRow.row, eliminateRow.col, eliminateRow.value)

    return null
}

fun runHumanBranchProof(board: TakuzuGrid): TakuzuHumanProofResult {
    val working: List<MutableList<TakuzuCellValue>> = board.map { it.toMutableList() }
    val steps = mutableListOf<TakuzuHumanProofStep>()

    while (true) {
        val contradiction = detectTriple(working) ?: detectBalanceOverflow(working) ?: detectDuplicateCompletedLine(working)
        if (contradiction != null) {
            return TakuzuHumanProofResult.Contradiction(steps.toList(), contradiction)
        }

        val step = findHumanProofStep(working) ?: return TakuzuHumanProofResult.Stalled(steps.toList())
        working[step.row][step.col] = step.value
        steps.add(step)
    }
}
