package com.quietgrid.app.games.blockescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.applyBlockEscapeMove
import com.quietgrid.engine.blockescape.blockEscapeLanesMatch
import com.quietgrid.engine.blockescape.isBlockEscapeSolved
import com.quietgrid.engine.blockescape.isValidBlockEscapeLayout

private const val BLOCKESCAPE_BASE_SCORE = 1000
private const val BLOCKESCAPE_HINT_PENALTY = 100
private const val BLOCKESCAPE_EXTRA_MOVE_PENALTY = 10

private fun blockEscapeSecondsPerPoint(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> 1
    Difficulty.MEDIUM -> 2
    Difficulty.HARD -> 3
    Difficulty.EXPERT -> 4
}

fun applyBlockEscapeSlide(session: BlockEscapeSession, move: BlockEscapeMove): BlockEscapeSession? {
    if (session.status != BlockEscapeStatus.PLAYING) return null
    val blocks = applyBlockEscapeMove(session.blocks, move) ?: return null
    return withBlocks(session.copy(moves = session.moves + 1), blocks)
}

fun chargeBlockEscapeHint(session: BlockEscapeSession): BlockEscapeSession? {
    if (session.status != BlockEscapeStatus.PLAYING) return null
    return session.copy(hintsUsed = session.hintsUsed + 1)
}

private fun withBlocks(session: BlockEscapeSession, blocks: List<BlockEscapeBlock>): BlockEscapeSession = session.copy(
    blocks = blocks,
    status = if (isBlockEscapeSolved(blocks)) BlockEscapeStatus.WON else BlockEscapeStatus.PLAYING,
)

fun blockEscapeAccuracy(session: BlockEscapeSession): Int {
    val minMoves = session.puzzle.minMoves
    if (session.moves <= 0) return 0
    return (minMoves * 100 / maxOf(session.moves, minMoves)).coerceIn(0, 100)
}

fun blockEscapeScore(session: BlockEscapeSession, elapsedSeconds: Int): Int {
    val extraMoves = maxOf(0, session.moves - session.puzzle.minMoves)
    val secondsPerPoint = blockEscapeSecondsPerPoint(Difficulty.fromKey(session.puzzle.difficulty))
    return maxOf(
        0,
        BLOCKESCAPE_BASE_SCORE -
            session.hintsUsed * BLOCKESCAPE_HINT_PENALTY -
            extraMoves * BLOCKESCAPE_EXTRA_MOVE_PENALTY -
            elapsedSeconds / secondsPerPoint,
    )
}

fun isRestorableBlockEscapeSession(session: BlockEscapeSession): Boolean =
    isValidBlockEscapeLayout(session.puzzle.blocks) &&
        isValidBlockEscapeLayout(session.blocks) &&
        blockEscapeLanesMatch(session.puzzle.blocks, session.blocks)
