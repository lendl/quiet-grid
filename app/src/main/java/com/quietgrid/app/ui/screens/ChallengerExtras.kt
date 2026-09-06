package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.ChallengerPuzzleSolve

data class ChallengerRunDetails(
    val puzzleHistory: List<ChallengerPuzzleSolve>,
    val solvesInTier: Int,
)

object ChallengerExtras {
    private var pending: ChallengerRunDetails? = null

    fun set(details: ChallengerRunDetails) {
        pending = details
    }

    fun consume(): ChallengerRunDetails = pending?.also { pending = null }
        ?: ChallengerRunDetails(puzzleHistory = emptyList(), solvesInTier = 0)
}
