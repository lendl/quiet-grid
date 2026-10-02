package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.ChallengerRunState

const val CHIMPTEST_CHALLENGER_STARTING_LIVES = 3
const val CHIMPTEST_CHALLENGER_STARTING_SECONDS = 90.0
const val CHIMPTEST_CHALLENGER_BONUS_SECONDS = 25.0
const val CHIMPTEST_CHALLENGER_SOLVES_PER_TIER = 3

data class ChimpTestChallengerSession(
    val puzzleSession: ChimpTestSession,
    val livesRemaining: Int,
    override val tier: Difficulty,
    override val solvesInTier: Int,
    override val puzzlesSolved: Int,
    override val score: Int,
    override val secondsRemaining: Double,
    val secondsOnCurrentPuzzle: Double,
    override val fastestSolveSeconds: Double? = null,
    override val puzzleHistory: List<ChallengerPuzzleSolve> = emptyList(),
) : ChallengerRunState
