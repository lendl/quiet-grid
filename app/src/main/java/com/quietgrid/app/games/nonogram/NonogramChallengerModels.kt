package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty

const val NONOGRAM_CHALLENGER_STARTING_LIVES = 3
const val NONOGRAM_CHALLENGER_STARTING_SECONDS = 110.0
const val NONOGRAM_CHALLENGER_BONUS_SECONDS = 20.0
const val NONOGRAM_CHALLENGER_SOLVES_PER_TIER = 3

data class NonogramChallengerSession(
    val puzzleSession: NonogramSession,
    val livesRemaining: Int,
    val tier: Difficulty,
    val solvesInTier: Int,
    val puzzlesSolved: Int,
    val score: Int,
    val secondsRemaining: Double,
    val secondsOnCurrentPuzzle: Double,
    val servedPuzzleIds: Set<String>,
    val fastestSolveSeconds: Double? = null,
    val puzzleHistory: List<ChallengerPuzzleSolve> = emptyList(),
)

data class NonogramChallengerResult(
    val puzzlesSolved: Int,
    val tierReached: Difficulty,
    val score: Int,
    val isNewHighScore: Boolean,
    val reason: String,
    val previousBest: Int,
    val fastestSolveSeconds: Double?,
    val puzzleHistory: List<ChallengerPuzzleSolve>,
    val solvesInTier: Int,
)
