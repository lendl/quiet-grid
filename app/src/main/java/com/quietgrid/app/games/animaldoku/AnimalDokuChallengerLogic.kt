package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry

fun createInitialChallengerSession(firstPuzzle: AnimalDokuPuzzleEntry): AnimalDokuChallengerSession =
    startChallengerSession(ANIMALDOKU_CHALLENGER_CONFIG, createAnimalDokuSession(firstPuzzle), firstPuzzle.id)

fun animalDokuChallengerPuzzleScore(session: AnimalDokuChallengerSession): Int =
    animalDokuScore(session.puzzleSession.lives, session.secondsOnCurrentPuzzle.toInt())

fun advanceChallengerAfterSolve(
    session: AnimalDokuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: AnimalDokuPuzzleEntry,
): AnimalDokuChallengerSession = session.advanceAfterSolve(
    config = ANIMALDOKU_CHALLENGER_CONFIG,
    gainedScore = animalDokuChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createAnimalDokuSession(nextPuzzle).copy(lives = session.puzzleSession.lives),
    nextPuzzleId = nextPuzzle.id,
)
