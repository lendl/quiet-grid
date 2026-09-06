package com.quietgrid.app.ui.components

import com.quietgrid.app.core.Difficulty

fun challengerTierFillFraction(segmentIndex: Int, tierReached: Difficulty, solvesInTier: Int, solvesPerTier: Int): Float =
    when {
        segmentIndex < tierReached.ordinal -> 1f
        segmentIndex > tierReached.ordinal -> 0f
        tierReached == Difficulty.EXPERT -> 1f
        else -> (solvesInTier.toFloat() / solvesPerTier.toFloat()).coerceIn(0f, 1f)
    }
