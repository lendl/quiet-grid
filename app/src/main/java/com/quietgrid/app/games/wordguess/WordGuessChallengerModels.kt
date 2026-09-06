package com.quietgrid.app.games.wordguess

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty

const val WORDGUESS_CHALLENGER_STARTING_LIVES = 3
const val WORDGUESS_CHALLENGER_STARTING_SECONDS = 150.0
const val WORDGUESS_CHALLENGER_BONUS_SECONDS = 35.0
const val WORDGUESS_CHALLENGER_SOLVES_PER_TIER = 3

data class WordGuessChallengerSession(
    val puzzleSession: WordGuessSession,
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

data class WordGuessChallengerResult(
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
