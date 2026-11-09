package com.quietgrid.app.games.wordswap

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapDirection
import com.quietgrid.engine.wordswap.WordSwapHint
import com.quietgrid.engine.wordswap.WordSwapMove
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.applyWordSwapMove
import com.quietgrid.engine.wordswap.cells
import com.quietgrid.engine.wordswap.findWordSwapHint
import com.quietgrid.engine.wordswap.isValidWordSwapBoard
import com.quietgrid.engine.wordswap.isWordSwapSolved
import com.quietgrid.engine.wordswap.read
import com.quietgrid.engine.wordswap.wordSwapSolvedSlots

private const val WORDSWAP_BASE_SCORE = 1000
private const val WORDSWAP_HINT_PENALTY = 100
private const val WORDSWAP_EXTRA_SWAP_PENALTY = 10

private fun wordSwapSecondsPerPoint(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> 1
    Difficulty.MEDIUM -> 2
    Difficulty.HARD -> 3
    Difficulty.EXPERT -> 4
}

data class WordSwapBankWord(val word: String, val direction: WordSwapDirection, val done: Boolean)

fun applyWordSwapTiles(session: WordSwapSession, move: WordSwapMove): WordSwapSession? {
    if (session.status != WordSwapStatus.PLAYING) return null
    val board = applyWordSwapMove(session.board, move) ?: return null
    if (board == session.board) return null
    return withBoard(session.copy(swaps = session.swaps + 1), board)
}

fun restartWordSwap(session: WordSwapSession): WordSwapSession? {
    if (session.status != WordSwapStatus.PLAYING || session.board == session.puzzle.start) return null
    return session.copy(board = session.puzzle.start)
}

fun undoWordSwap(current: WordSwapSession, previous: WordSwapSession): WordSwapSession =
    previous.copy(swaps = current.swaps, hintsUsed = current.hintsUsed)

fun findWordSwapSessionHint(session: WordSwapSession): WordSwapHint? {
    if (session.status != WordSwapStatus.PLAYING) return null
    return findWordSwapHint(session.puzzle, session.board)
}

fun wordSwapHintCells(puzzle: WordSwapPuzzleEntry, hint: WordSwapHint?): Set<Int> = when (hint) {
    is WordSwapHint.Placement -> puzzle.slots[hint.slotIndex].cells(puzzle.cols).toSet()
    is WordSwapHint.Swap -> setOf(hint.move.first, hint.move.second)
    null -> emptySet()
}

private fun withBoard(session: WordSwapSession, board: String): WordSwapSession = session.copy(
    board = board,
    status = if (isWordSwapSolved(session.puzzle, board)) WordSwapStatus.WON else WordSwapStatus.PLAYING,
)

fun wordSwapDoneCells(session: WordSwapSession): Set<Int> {
    val puzzle = session.puzzle
    return wordSwapSolvedSlots(puzzle, session.board).flatMapTo(HashSet()) { index -> puzzle.slots[index].cells(puzzle.cols) }
}

fun wordSwapBank(session: WordSwapSession): List<WordSwapBankWord> {
    val puzzle = session.puzzle
    val solved = wordSwapSolvedSlots(puzzle, session.board)
    return puzzle.slots.mapIndexed { index, slot ->
        WordSwapBankWord(slot.read(puzzle.solution, puzzle.cols), slot.direction, index in solved)
    }.sortedWith(compareBy({ it.direction.ordinal }, { it.word }))
}

fun wordSwapAccuracy(session: WordSwapSession): Int {
    val minSwaps = session.puzzle.minSwaps
    if (session.swaps <= 0) return 0
    return (minSwaps * 100 / maxOf(session.swaps, minSwaps)).coerceIn(0, 100)
}

fun wordSwapScore(session: WordSwapSession, elapsedSeconds: Int): Int {
    val extraSwaps = maxOf(0, session.swaps - session.puzzle.minSwaps)
    val secondsPerPoint = wordSwapSecondsPerPoint(Difficulty.fromKey(session.puzzle.difficulty))
    return maxOf(
        0,
        WORDSWAP_BASE_SCORE -
            session.hintsUsed * WORDSWAP_HINT_PENALTY -
            extraSwaps * WORDSWAP_EXTRA_SWAP_PENALTY -
            elapsedSeconds / secondsPerPoint,
    )
}

fun isRestorableWordSwapSession(session: WordSwapSession): Boolean =
    isValidWordSwapBoard(session.puzzle, session.puzzle.start) && isValidWordSwapBoard(session.puzzle, session.board)
