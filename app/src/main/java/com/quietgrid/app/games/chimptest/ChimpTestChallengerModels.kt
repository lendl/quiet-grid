package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty

const val CHIMPTEST_CHALLENGER_STARTING_LIVES = 3
const val CHIMPTEST_CHALLENGER_STARTING_SECONDS = 90.0
const val CHIMPTEST_CHALLENGER_BONUS_SECONDS = 25.0
const val CHIMPTEST_CHALLENGER_SOLVES_PER_TIER = 3

data class ChimpTestChallengerSession(
    val puzzleSession: ChimpTestSession,
    val livesRemaining: Int,
    val tier: Difficulty,
    val solvesInTier: Int,
    val puzzlesSolved: Int,
    val score: Int,
    val secondsRemaining: Double,
    val secondsOnCurrentPuzzle: Double,
    val fastestSolveSeconds: Double? = null,
    val puzzleHistory: List<ChallengerPuzzleSolve> = emptyList(),
)

data class ChimpTestChallengerResult(
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
