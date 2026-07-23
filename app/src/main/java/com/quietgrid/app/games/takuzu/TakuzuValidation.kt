package com.quietgrid.app.games.takuzu

/** Line key like "r0" or "c3". */
typealias LineKey = String

private fun lineIndexOf(lineKey: LineKey): Int = lineKey.substring(1).toInt()
private fun isRowKey(lineKey: LineKey): Boolean = lineKey.startsWith("r")

private fun rowOf(board: TakuzuGrid, index: Int): List<TakuzuCellValue> = board[index]
private fun colOf(board: TakuzuGrid, index: Int): List<TakuzuCellValue> = board.map { it[index] }

private fun completedLineState(line: List<TakuzuCellValue>, solLine: List<TakuzuCellValue>): CompletedLineState {
    if (line.any { it == null }) return CompletedLineState.INCOMPLETE
    return if (line.indices.all { line[it] == solLine[it] }) CompletedLineState.CORRECT else CompletedLineState.INCORRECT
}

fun getCompletedLineStateForKey(board: TakuzuGrid, solution: TakuzuGrid, lineKey: LineKey): CompletedLineState {
    val index = lineIndexOf(lineKey)
    return if (isRowKey(lineKey)) {
        completedLineState(rowOf(board, index), rowOf(solution, index))
    } else {
        completedLineState(colOf(board, index), colOf(solution, index))
    }
}

data class TouchedLineStates(val rowState: CompletedLineState, val colState: CompletedLineState)

fun getTouchedLineStates(board: TakuzuGrid, solution: TakuzuGrid, row: Int, col: Int): TouchedLineStates =
    TouchedLineStates(
        rowState = completedLineState(rowOf(board, row), rowOf(solution, row)),
        colState = completedLineState(colOf(board, col), colOf(solution, col)),
    )

fun getMismatchedCompletedLines(board: TakuzuGrid, solution: TakuzuGrid): List<LineKey> {
    val n = board.size
    val result = mutableListOf<LineKey>()
    for (r in 0 until n) {
        if (getCompletedLineStateForKey(board, solution, "r$r") == CompletedLineState.INCORRECT) result.add("r$r")
    }
    for (c in 0 until n) {
        if (getCompletedLineStateForKey(board, solution, "c$c") == CompletedLineState.INCORRECT) result.add("c$c")
    }
    return result
}

fun isBoardSolved(board: TakuzuGrid, solution: TakuzuGrid): Boolean =
    board.indices.all { r -> board[r].indices.all { c -> board[r][c] == solution[r][c] } }
