package com.quietgrid.app.games.starbattle

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry

private val STARBATTLE_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialStarBattleChallengerSession(firstPuzzle: StarBattlePuzzleEntry): StarBattleChallengerSession =
    StarBattleChallengerSession(
        puzzleSession = createStarBattleSession(firstPuzzle),
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = STARBATTLE_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun starBattleChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = STARBATTLE_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= STARBATTLE_CHALLENGER_SOLVES_PER_TIER && currentIndex < STARBATTLE_CHALLENGER_TIER_ORDER.lastIndex) {
        STARBATTLE_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun starBattleChallengerFastestSolve(session: StarBattleChallengerSession): Double =
    session.fastestSolveSeconds?.let { minOf(it, session.secondsOnCurrentPuzzle) } ?: session.secondsOnCurrentPuzzle

fun advanceStarBattleChallengerAfterSolve(
    session: StarBattleChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: StarBattlePuzzleEntry,
): StarBattleChallengerSession {
    val livesCarried = session.puzzleSession.lives
    val gainedScore = starBattleScore(livesCarried, session.secondsOnCurrentPuzzle.toInt())
    val fastestSolveSeconds = starBattleChallengerFastestSolve(session)
    return session.copy(
        puzzleSession = createStarBattleSession(nextPuzzle).copy(lives = livesCarried),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + STARBATTLE_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
        fastestSolveSeconds = fastestSolveSeconds,
        puzzleHistory = session.puzzleHistory + ChallengerPuzzleSolve(session.tier, session.secondsOnCurrentPuzzle),
    )
}

fun tickStarBattleChallenger(session: StarBattleChallengerSession): StarBattleChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
