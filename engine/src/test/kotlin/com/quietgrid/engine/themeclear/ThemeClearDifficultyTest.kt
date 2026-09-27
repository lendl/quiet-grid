package com.quietgrid.engine.themeclear

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeClearDifficultyTest {

    @Test
    fun `letter bands of all tiers are disjoint and ascending`() {
        val bands = Difficulty.entries.map { THEMECLEAR_TIERS.getValue(it).letters }
        bands.zipWithNext().forEach { (lower, upper) -> assertTrue(lower.last < upper.first) }
    }

    @Test
    fun `easy accepts a small grid that blind play usually clears`() {
        val spec = THEMECLEAR_TIERS.getValue(Difficulty.EASY)
        assertTrue(themeClearMeetsTier(Difficulty.EASY, spec.letters.first, ThemeClearMetrics(finishCount = 3, trapWords = 1, spellableWords = 6, blindSuccessRate = spec.maxBlindSuccess)))
    }

    @Test
    fun `easy rejects a letter count outside its band`() {
        assertFalse(themeClearMeetsTier(Difficulty.EASY, 20, ThemeClearMetrics(finishCount = 10, trapWords = 0, spellableWords = 8, blindSuccessRate = 0.6)))
    }

    @Test
    fun `no tier accepts an unsolvable puzzle`() {
        Difficulty.entries.forEach { difficulty ->
            val letters = THEMECLEAR_TIERS.getValue(difficulty).letters.first
            assertFalse(themeClearMeetsTier(difficulty, letters, ThemeClearMetrics(finishCount = 0, trapWords = 5, spellableWords = 10)))
        }
    }

    @Test
    fun `every tier rejects blind success below its floor or above its ceiling`() {
        Difficulty.entries.forEach { difficulty ->
            val spec = THEMECLEAR_TIERS.getValue(difficulty)
            val letters = spec.letters.first
            if (spec.minBlindSuccess > 0.0) {
                assertFalse(themeClearMeetsTier(difficulty, letters, ThemeClearMetrics(1, 0, 5, blindSuccessRate = spec.minBlindSuccess - 0.01)))
            }
            if (spec.maxBlindSuccess < 1.0) {
                assertFalse(themeClearMeetsTier(difficulty, letters, ThemeClearMetrics(1, 0, 5, blindSuccessRate = spec.maxBlindSuccess + 0.01)))
            }
        }
    }

    @Test
    fun `blind success windows get harder tier by tier`() {
        val specs = Difficulty.entries.map { THEMECLEAR_TIERS.getValue(it) }
        specs.zipWithNext().forEach { (easier, harder) -> assertTrue(harder.maxBlindSuccess <= easier.maxBlindSuccess && harder.minBlindSuccess <= easier.minBlindSuccess) }
    }
}
