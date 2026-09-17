package com.quietgrid.app.games.takuzu

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry

private val TAKUZU_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

fun createInitialTakuzuChallengerSession(firstPuzzle: TakuzuPuzzleEntry): TakuzuChallengerSession =
    TakuzuChallengerSession(
        puzzleSession = createTakuzuSession(firstPuzzle),
        livesRemaining = TAKUZU_CHALLENGER_STARTING_LIVES,
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = TAKUZU_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun takuzuChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = TAKUZU_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= TAKUZU_CHALLENGER_SOLVES_PER_TIER && currentIndex < TAKUZU_CHALLENGER_TIER_ORDER.lastIndex) {
        TAKUZU_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun takuzuChallengerFastestSolve(session: TakuzuChallengerSession): Double =
    session.fastestSolveSeconds?.let { minOf(it, session.secondsOnCurrentPuzzle) } ?: session.secondsOnCurrentPuzzle

fun advanceTakuzuChallengerAfterSolve(
    session: TakuzuChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: TakuzuPuzzleEntry,
): TakuzuChallengerSession {
    val gainedScore = takuzuScore(session.tier, session.secondsOnCurrentPuzzle.toInt(), session.puzzleSession.accuracyDrops)
    val fastestSolveSeconds = takuzuChallengerFastestSolve(session)
    return session.copy(
        puzzleSession = createTakuzuSession(nextPuzzle),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + TAKUZU_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
        fastestSolveSeconds = fastestSolveSeconds,
        puzzleHistory = session.puzzleHistory + ChallengerPuzzleSolve(session.tier, session.secondsOnCurrentPuzzle),
    )
}

fun tickTakuzuChallenger(session: TakuzuChallengerSession): TakuzuChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
