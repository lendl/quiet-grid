package com.quietgrid.engine.takuzu

enum class TakuzuTechnique { FIND_PAIRS, AVOID_TRIOS, COMPLETE_LINES, ELIMINATE_FILLED_LINES, ELIMINATE_IMPOSSIBLE_COMBINATIONS }

data class TakuzuMove(val row: Int, val col: Int, val value: Int, val technique: TakuzuTechnique)

fun findPairMoveInLine(line: List<TakuzuCellValue>): Pair<Int, Int>? {
    for (index in 0..line.size - 3) {
        val first = line[index]
        val second = line[index + 1]
        val third = line[index + 2]
        if (first != null && first == second && third == null) return (index + 2) to otherValue(first)
        if (first == null && second != null && second == third) return index to otherValue(second)
    }
    return null
}

fun findAvoidTrioMoveInLine(line: List<TakuzuCellValue>): Pair<Int, Int>? {
    for (index in 0..line.size - 3) {
        val first = line[index]
        val second = line[index + 1]
        val third = line[index + 2]
        if (first != null && first == third && second == null) return (index + 1) to otherValue(first)
    }
    return null
}

fun findPairsMove(board: TakuzuGrid): TakuzuMove? {
    val size = board.size
    for (row in 0 until size) {
        val move = findPairMoveInLine(board[row]) ?: continue
        return TakuzuMove(row, move.first, move.second, TakuzuTechnique.FIND_PAIRS)
    }
    for (col in 0 until size) {
        val move = findPairMoveInLine(getColumn(board, col)) ?: continue
        return TakuzuMove(move.first, col, move.second, TakuzuTechnique.FIND_PAIRS)
    }
    return null
}

fun findAvoidTriosMove(board: TakuzuGrid): TakuzuMove? {
    val size = board.size
    for (row in 0 until size) {
        val move = findAvoidTrioMoveInLine(board[row]) ?: continue
        return TakuzuMove(row, move.first, move.second, TakuzuTechnique.AVOID_TRIOS)
    }
    for (col in 0 until size) {
        val move = findAvoidTrioMoveInLine(getColumn(board, col)) ?: continue
        return TakuzuMove(move.first, col, move.second, TakuzuTechnique.AVOID_TRIOS)
    }
    return null
}

fun findCompleteLinesMove(board: TakuzuGrid): TakuzuMove? {
    val size = board.size
    val half = size / 2
    for (row in 0 until size) {
        val line = board[row]
        val zeroes = countValue(line, 0)
        val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val fillValue = if (zeroes == half) 1 else 0
            val col = line.indexOfFirst { it == null }
            if (col != -1) return TakuzuMove(row, col, fillValue, TakuzuTechnique.COMPLETE_LINES)
        }
    }
    for (col in 0 until size) {
        val line = getColumn(board, col)
        val zeroes = countValue(line, 0)
        val ones = countValue(line, 1)
        if (zeroes == half || ones == half) {
            val fillValue = if (zeroes == half) 1 else 0
            val row = line.indexOfFirst { it == null }
            if (row != -1) return TakuzuMove(row, col, fillValue, TakuzuTechnique.COMPLETE_LINES)
        }
    }
    return null
}

private fun findEliminateFilledLinesRowMove(board: TakuzuGrid): TakuzuMove? {
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
            return TakuzuMove(row, col, otherValue(board[complete][col]!!), TakuzuTechnique.ELIMINATE_FILLED_LINES)
        }
    }
    return null
}

private fun findEliminateFilledLinesColumnMove(board: TakuzuGrid): TakuzuMove? {
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
            return TakuzuMove(row, col, otherValue(completeLine[row]!!), TakuzuTechnique.ELIMINATE_FILLED_LINES)
        }
    }
    return null
}

fun findEliminateFilledLinesMove(board: TakuzuGrid): TakuzuMove? =
    findEliminateFilledLinesRowMove(board) ?: findEliminateFilledLinesColumnMove(board)
