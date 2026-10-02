// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerModels.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.ChallengerRunState

const val ANIMALDOKU_CHALLENGER_STARTING_SECONDS = 90.0
const val ANIMALDOKU_CHALLENGER_BONUS_SECONDS = 15.0
const val ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER = 3

data class AnimalDokuChallengerSession(
    val puzzleSession: AnimalDokuSession,
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
