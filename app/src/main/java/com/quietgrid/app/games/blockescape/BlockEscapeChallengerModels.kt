package com.quietgrid.app.games.blockescape

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.ChallengerRunState

const val BLOCKESCAPE_CHALLENGER_STARTING_SECONDS = 120.0
const val BLOCKESCAPE_CHALLENGER_BONUS_SECONDS = 30.0
const val BLOCKESCAPE_CHALLENGER_SOLVES_PER_TIER = 3

data class BlockEscapeChallengerSession(
    val puzzleSession: BlockEscapeSession,
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
