// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerModels.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty

const val ANIMALDOKU_CHALLENGER_STARTING_SECONDS = 90.0
const val ANIMALDOKU_CHALLENGER_BONUS_SECONDS = 15.0
const val ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER = 3

data class AnimalDokuChallengerSession(
    val puzzleSession: AnimalDokuSession,
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

data class AnimalDokuChallengerResult(
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
