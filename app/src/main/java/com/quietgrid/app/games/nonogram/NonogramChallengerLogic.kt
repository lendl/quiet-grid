package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry

private val NONOGRAM_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialNonogramChallengerSession(firstPuzzle: NonogramPuzzleEntry): NonogramChallengerSession =
    NonogramChallengerSession(
        puzzleSession = createNonogramSession(firstPuzzle),
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = NONOGRAM_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun nonogramChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = NONOGRAM_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= NONOGRAM_CHALLENGER_SOLVES_PER_TIER && currentIndex < NONOGRAM_CHALLENGER_TIER_ORDER.lastIndex) {
        NONOGRAM_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun nonogramChallengerFastestSolve(session: NonogramChallengerSession): Double =
    session.fastestSolveSeconds?.let { minOf(it, session.secondsOnCurrentPuzzle) } ?: session.secondsOnCurrentPuzzle

fun advanceNonogramChallengerAfterSolve(
    session: NonogramChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: NonogramPuzzleEntry,
): NonogramChallengerSession {
    val gainedScore = nonogramScore(session.secondsOnCurrentPuzzle.toInt())
    val fastestSolveSeconds = nonogramChallengerFastestSolve(session)
    return session.copy(
        puzzleSession = createNonogramSession(nextPuzzle),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + NONOGRAM_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
        fastestSolveSeconds = fastestSolveSeconds,
        puzzleHistory = session.puzzleHistory + ChallengerPuzzleSolve(session.tier, session.secondsOnCurrentPuzzle),
    )
}

fun tickNonogramChallenger(session: NonogramChallengerSession): NonogramChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
