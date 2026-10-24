package com.quietgrid.app.ui.analyzer

private const val MAX_ANALYZER_STEPS = 400

data class AnalyzerReplay<Puzzle, Board, Hint>(
    val puzzle: Puzzle,
    val steps: List<AnalyzerStep<Board, Hint>>,
    val finalBoard: Board,
)

sealed interface AnalyzerState<out Puzzle, out Board, out Hint> {
    data object Loading : AnalyzerState<Nothing, Nothing, Nothing>
    data object LoadFailed : AnalyzerState<Nothing, Nothing, Nothing>
    data object AlreadySolved : AnalyzerState<Nothing, Nothing, Nothing>
    data class Ready<Puzzle, Board, Hint>(
        val replay: AnalyzerReplay<Puzzle, Board, Hint>,
        val currentIndex: Int = 0,
        val isPlaying: Boolean = false,
    ) : AnalyzerState<Puzzle, Board, Hint> {
        val isFinalFrame: Boolean get() = currentIndex == replay.steps.size
        val board: Board get() = if (isFinalFrame) replay.finalBoard else replay.steps[currentIndex].boardBefore
        val hint: Hint? get() = if (isFinalFrame) null else replay.steps[currentIndex].hint
    }
}

fun <Puzzle, Board, Hint> buildAnalyzerState(
    puzzle: Puzzle,
    initialBoard: Board,
    isSolved: (Board) -> Boolean,
    nextHint: (Board) -> Hint?,
    applyHint: (Board, Hint) -> Board,
    maxSteps: Int = MAX_ANALYZER_STEPS,
): AnalyzerState<Puzzle, Board, Hint> {
    if (isSolved(initialBoard)) return AnalyzerState.AlreadySolved
    val steps = replayAnalyzerSteps(initialBoard, isSolved, nextHint, applyHint, maxSteps)
    val finalBoard = steps.lastOrNull()?.let { applyHint(it.boardBefore, it.hint) } ?: initialBoard
    if (!isSolved(finalBoard)) return AnalyzerState.LoadFailed
    return AnalyzerState.Ready(AnalyzerReplay(puzzle, steps, finalBoard))
}
