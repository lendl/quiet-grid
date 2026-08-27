package com.quietgrid.app.ui.analyzer

data class AnalyzerStep<Board, Hint>(val boardBefore: Board, val hint: Hint)

fun <Board, Hint> replayAnalyzerSteps(
    initialBoard: Board,
    isSolved: (Board) -> Boolean,
    nextHint: (Board) -> Hint?,
    applyHint: (Board, Hint) -> Board,
    maxSteps: Int,
): List<AnalyzerStep<Board, Hint>> {
    val steps = mutableListOf<AnalyzerStep<Board, Hint>>()
    var board = initialBoard
    while (!isSolved(board) && steps.size < maxSteps) {
        val hint = nextHint(board) ?: break
        steps.add(AnalyzerStep(board, hint))
        board = applyHint(board, hint)
    }
    return steps
}
