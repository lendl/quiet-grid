package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry

fun createInitialNonogramChallengerSession(firstPuzzle: NonogramPuzzleEntry): NonogramChallengerSession =
    startChallengerSession(NONOGRAM_CHALLENGER_CONFIG, createNonogramSession(firstPuzzle), firstPuzzle.id)

fun nonogramChallengerPuzzleScore(session: NonogramChallengerSession): Int =
    nonogramScore(session.secondsOnCurrentPuzzle.toInt())

fun advanceNonogramChallengerAfterSolve(
    session: NonogramChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: NonogramPuzzleEntry,
): NonogramChallengerSession = session.advanceAfterSolve(
    config = NONOGRAM_CHALLENGER_CONFIG,
    gainedScore = nonogramChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createNonogramSession(nextPuzzle),
    nextPuzzleId = nextPuzzle.id,
)
