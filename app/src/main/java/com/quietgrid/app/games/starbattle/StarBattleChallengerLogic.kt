package com.quietgrid.app.games.starbattle

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry

fun createInitialStarBattleChallengerSession(firstPuzzle: StarBattlePuzzleEntry): StarBattleChallengerSession =
    startChallengerSession(STARBATTLE_CHALLENGER_CONFIG, createStarBattleSession(firstPuzzle), firstPuzzle.id)

fun starBattleChallengerPuzzleScore(session: StarBattleChallengerSession): Int =
    starBattleScore(session.puzzleSession.lives, session.secondsOnCurrentPuzzle.toInt())

fun advanceStarBattleChallengerAfterSolve(
    session: StarBattleChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: StarBattlePuzzleEntry,
): StarBattleChallengerSession = session.advanceAfterSolve(
    config = STARBATTLE_CHALLENGER_CONFIG,
    gainedScore = starBattleChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createStarBattleSession(nextPuzzle).copy(lives = session.puzzleSession.lives),
    nextPuzzleId = nextPuzzle.id,
)
