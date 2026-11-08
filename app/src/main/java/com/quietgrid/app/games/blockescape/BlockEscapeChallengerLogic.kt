package com.quietgrid.app.games.blockescape

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry

private val BLOCKESCAPE_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialBlockEscapeChallengerSession(firstPuzzle: BlockEscapePuzzleEntry): BlockEscapeChallengerSession =
    BlockEscapeChallengerSession(
        puzzleSession = createBlockEscapeSession(firstPuzzle),
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = BLOCKESCAPE_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun blockEscapeChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = BLOCKESCAPE_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= BLOCKESCAPE_CHALLENGER_SOLVES_PER_TIER && currentIndex < BLOCKESCAPE_CHALLENGER_TIER_ORDER.lastIndex) {
        BLOCKESCAPE_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun blockEscapeChallengerFastestSolve(session: BlockEscapeChallengerSession): Double =
    session.fastestSolveSeconds?.let { minOf(it, session.secondsOnCurrentPuzzle) } ?: session.secondsOnCurrentPuzzle

fun blockEscapeChallengerPuzzleScore(session: BlockEscapeChallengerSession): Int =
    blockEscapeScore(session.puzzleSession, session.secondsOnCurrentPuzzle.toInt())

fun creditBlockEscapeChallengerSolve(session: BlockEscapeChallengerSession): BlockEscapeChallengerSession = session.copy(
    puzzlesSolved = session.puzzlesSolved + 1,
    score = session.score + blockEscapeChallengerPuzzleScore(session),
    fastestSolveSeconds = blockEscapeChallengerFastestSolve(session),
    puzzleHistory = session.puzzleHistory + ChallengerPuzzleSolve(session.tier, session.secondsOnCurrentPuzzle),
)

fun advanceBlockEscapeChallengerAfterSolve(
    session: BlockEscapeChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: BlockEscapePuzzleEntry,
): BlockEscapeChallengerSession = creditBlockEscapeChallengerSolve(session).copy(
    puzzleSession = createBlockEscapeSession(nextPuzzle),
    tier = nextTier,
    solvesInTier = nextSolvesInTier,
    secondsRemaining = session.secondsRemaining + BLOCKESCAPE_CHALLENGER_BONUS_SECONDS,
    secondsOnCurrentPuzzle = 0.0,
    servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
)

fun tickBlockEscapeChallenger(session: BlockEscapeChallengerSession): BlockEscapeChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
