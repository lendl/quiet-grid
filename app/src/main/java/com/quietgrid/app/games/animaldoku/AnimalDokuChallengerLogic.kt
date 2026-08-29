// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerLogic.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry

private val ANIMALDOKU_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialChallengerSession(firstPuzzle: AnimalDokuPuzzleEntry): AnimalDokuChallengerSession =
    AnimalDokuChallengerSession(
        puzzleSession = createAnimalDokuSession(firstPuzzle),
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = ANIMALDOKU_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun tierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = ANIMALDOKU_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER && currentIndex < ANIMALDOKU_CHALLENGER_TIER_ORDER.lastIndex) {
        ANIMALDOKU_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun advanceChallengerAfterSolve(
    session: AnimalDokuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: AnimalDokuPuzzleEntry,
): AnimalDokuChallengerSession {
    val livesCarried = session.puzzleSession.lives
    val gainedScore = animalDokuScore(livesCarried, session.secondsOnCurrentPuzzle.toInt())
    return session.copy(
        puzzleSession = createAnimalDokuSession(nextPuzzle).copy(lives = livesCarried),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + ANIMALDOKU_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
    )
}

fun tickChallenger(session: AnimalDokuChallengerSession): AnimalDokuChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
