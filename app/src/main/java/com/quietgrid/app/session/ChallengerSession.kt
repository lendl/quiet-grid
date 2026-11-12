package com.quietgrid.app.session

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty

private val CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

data class ChallengerSession<P>(
    val puzzleSession: P,
    override val tier: Difficulty,
    override val solvesInTier: Int,
    override val puzzlesSolved: Int,
    override val score: Int,
    override val secondsRemaining: Double,
    val secondsOnCurrentPuzzle: Double,
    val servedPuzzleIds: Set<String> = emptySet(),
    val livesRemaining: Int = 0,
    override val fastestSolveSeconds: Double? = null,
    override val puzzleHistory: List<ChallengerPuzzleSolve> = emptyList(),
) : ChallengerRunState

fun <P> startChallengerSession(config: ChallengerConfig, puzzleSession: P, puzzleId: String? = null): ChallengerSession<P> =
    ChallengerSession(
        puzzleSession = puzzleSession,
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = config.startingSeconds,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOfNotNull(puzzleId),
        livesRemaining = config.startingLives,
    )

fun challengerTierAfterSolve(config: ChallengerConfig, currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= config.solvesPerTier && currentIndex < CHALLENGER_TIER_ORDER.lastIndex) {
        CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun <P> ChallengerSession<P>.fastestSolveIncludingCurrent(): Double =
    fastestSolveSeconds?.let { minOf(it, secondsOnCurrentPuzzle) } ?: secondsOnCurrentPuzzle

fun <P> ChallengerSession<P>.creditSolve(gainedScore: Int): ChallengerSession<P> = copy(
    puzzlesSolved = puzzlesSolved + 1,
    score = score + gainedScore,
    fastestSolveSeconds = fastestSolveIncludingCurrent(),
    puzzleHistory = puzzleHistory + ChallengerPuzzleSolve(tier, secondsOnCurrentPuzzle),
)

fun <P> ChallengerSession<P>.advanceAfterSolve(
    config: ChallengerConfig,
    gainedScore: Int,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzleSession: P,
    nextPuzzleId: String? = null,
): ChallengerSession<P> = creditSolve(gainedScore).copy(
    puzzleSession = nextPuzzleSession,
    tier = nextTier,
    solvesInTier = nextSolvesInTier,
    secondsRemaining = secondsRemaining + config.bonusSeconds,
    secondsOnCurrentPuzzle = 0.0,
    servedPuzzleIds = servedPuzzleIds + setOfNotNull(nextPuzzleId),
)

fun <P> ChallengerSession<P>.tick(): ChallengerSession<P> = copy(
    secondsRemaining = secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = secondsOnCurrentPuzzle + 1.0,
)
