package com.quietgrid.app.games.wordguess

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.ChallengerRunState

const val WORDGUESS_CHALLENGER_STARTING_LIVES = 3
const val WORDGUESS_CHALLENGER_STARTING_SECONDS = 150.0
const val WORDGUESS_CHALLENGER_BONUS_SECONDS = 35.0
const val WORDGUESS_CHALLENGER_SOLVES_PER_TIER = 3

data class WordGuessChallengerSession(
    val puzzleSession: WordGuessSession,
    val livesRemaining: Int,
    override val tier: Difficulty,
    override val solvesInTier: Int,
    override val puzzlesSolved: Int,
    override val score: Int,
    override val secondsRemaining: Double,
    val secondsOnCurrentPuzzle: Double,
    val servedPuzzleIds: Set<String>,
    override val fastestSolveSeconds: Double? = null,
    override val puzzleHistory: List<ChallengerPuzzleSolve> = emptyList(),
) : ChallengerRunState
