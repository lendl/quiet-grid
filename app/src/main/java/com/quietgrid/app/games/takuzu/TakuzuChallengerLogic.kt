package com.quietgrid.app.games.takuzu

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry

fun createInitialTakuzuChallengerSession(firstPuzzle: TakuzuPuzzleEntry): TakuzuChallengerSession =
    startChallengerSession(TAKUZU_CHALLENGER_CONFIG, createTakuzuSession(firstPuzzle), firstPuzzle.id)

fun takuzuChallengerPuzzleScore(session: TakuzuChallengerSession): Int =
    takuzuScore(session.tier, session.secondsOnCurrentPuzzle.toInt(), session.puzzleSession.accuracyDrops)

fun advanceTakuzuChallengerAfterSolve(
    session: TakuzuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: TakuzuPuzzleEntry,
): TakuzuChallengerSession = session.advanceAfterSolve(
    config = TAKUZU_CHALLENGER_CONFIG,
    gainedScore = takuzuChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createTakuzuSession(nextPuzzle),
    nextPuzzleId = nextPuzzle.id,
)
