package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession

fun createInitialChimpTestChallengerSession(): ChimpTestChallengerSession =
    startChallengerSession(CHIMPTEST_CHALLENGER_CONFIG, createChimpTestSession(Difficulty.EASY))

fun chimpTestChallengerPuzzleScore(session: ChimpTestChallengerSession): Int = chimpTestScore(session.puzzleSession)

fun advanceChimpTestChallengerAfterSolve(
    session: ChimpTestChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
): ChimpTestChallengerSession = session.advanceAfterSolve(
    config = CHIMPTEST_CHALLENGER_CONFIG,
    gainedScore = chimpTestChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createChimpTestSession(nextTier),
)

fun advanceChimpTestChallengerAfterLoss(session: ChimpTestChallengerSession): ChimpTestChallengerSession =
    session.copy(
        puzzleSession = createChimpTestSession(session.tier),
        secondsOnCurrentPuzzle = 0.0,
    )
