package com.quietgrid.app.ui.components

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengerTierFillTest {

    @Test
    fun `a tier before the reached tier is fully filled`() {
        assertEquals(1f, challengerTierFillFraction(0, Difficulty.HARD, solvesInTier = 1, solvesPerTier = 3), 0f)
        assertEquals(1f, challengerTierFillFraction(1, Difficulty.HARD, solvesInTier = 1, solvesPerTier = 3), 0f)
    }

    @Test
    fun `a tier after the reached tier is empty`() {
        assertEquals(0f, challengerTierFillFraction(3, Difficulty.HARD, solvesInTier = 1, solvesPerTier = 3), 0f)
    }

    @Test
    fun `the reached tier fills proportionally to solves in it`() {
        assertEquals(2f / 3f, challengerTierFillFraction(2, Difficulty.HARD, solvesInTier = 2, solvesPerTier = 3), 0.0001f)
        assertEquals(0f, challengerTierFillFraction(2, Difficulty.HARD, solvesInTier = 0, solvesPerTier = 3), 0f)
    }

    @Test
    fun `expert is always fully filled once reached regardless of solve count`() {
        assertEquals(1f, challengerTierFillFraction(3, Difficulty.EXPERT, solvesInTier = 0, solvesPerTier = 3), 0f)
        assertEquals(1f, challengerTierFillFraction(3, Difficulty.EXPERT, solvesInTier = 40, solvesPerTier = 3), 0f)
    }
}
