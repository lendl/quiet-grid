package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.Difficulty

private val CHIMPTEST_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialChimpTestChallengerSession(): ChimpTestChallengerSession =
    ChimpTestChallengerSession(
        puzzleSession = createChimpTestSession(Difficulty.EASY),
        livesRemaining = CHIMPTEST_CHALLENGER_STARTING_LIVES,
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = CHIMPTEST_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
    )

fun chimpTestChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = CHIMPTEST_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= CHIMPTEST_CHALLENGER_SOLVES_PER_TIER && currentIndex < CHIMPTEST_CHALLENGER_TIER_ORDER.lastIndex) {
        CHIMPTEST_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun advanceChimpTestChallengerAfterSolve(
    session: ChimpTestChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
): ChimpTestChallengerSession {
    val gainedScore = chimpTestScore(session.puzzleSession)
    return session.copy(
        puzzleSession = createChimpTestSession(nextTier),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + CHIMPTEST_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
    )
}

fun advanceChimpTestChallengerAfterLoss(session: ChimpTestChallengerSession): ChimpTestChallengerSession =
    session.copy(
        puzzleSession = createChimpTestSession(session.tier),
        secondsOnCurrentPuzzle = 0.0,
    )

fun tickChimpTestChallenger(session: ChimpTestChallengerSession): ChimpTestChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
