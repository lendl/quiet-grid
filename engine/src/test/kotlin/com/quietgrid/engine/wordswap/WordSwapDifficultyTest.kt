package com.quietgrid.engine.wordswap

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSwapDifficultyTest {

    private val solved = WordSwapSolveResult(true, emptyList())

    @Test
    fun unsolvedGetsNoTier() {
        assertNull(classifyWordSwapDifficulty(GRID3, WordSwapSolveResult(false, emptyList())))
    }

    @Test
    fun swapBandsSplitAfterNineFifteenAndTwenty() {
        assertEquals(Difficulty.EASY, wordSwapSwapBand(9))
        assertEquals(Difficulty.MEDIUM, wordSwapSwapBand(10))
        assertEquals(Difficulty.MEDIUM, wordSwapSwapBand(15))
        assertEquals(Difficulty.HARD, wordSwapSwapBand(16))
        assertEquals(Difficulty.HARD, wordSwapSwapBand(20))
        assertEquals(Difficulty.EXPERT, wordSwapSwapBand(21))
    }

    @Test
    fun boardSizeOutsideTheBandsTierGetsNoTier() {
        assertNull(classifyWordSwapDifficulty(GRID3.copy(minSwaps = 12), solved))
        assertNull(classifyWordSwapDifficulty(GRID3.copy(rows = 5, cols = 5, minSwaps = 21), solved))
    }

    @Test
    fun expertNeedsSixBySix() {
        assertEquals(Difficulty.EXPERT, classifyWordSwapDifficulty(GRID3.copy(rows = 6, cols = 6, minSwaps = 21), solved))
    }

    @Test
    fun hardTakesFiveAndSixBySix() {
        assertEquals(Difficulty.HARD, classifyWordSwapDifficulty(GRID3.copy(rows = 5, cols = 5, minSwaps = 18), solved))
        assertEquals(Difficulty.HARD, classifyWordSwapDifficulty(GRID3.copy(rows = 6, cols = 6, minSwaps = 18), solved))
    }

    @Test
    fun grid3GradesEasy() {
        assertEquals(6, GRID3.minSwaps)
        assertEquals(Difficulty.EASY, gradeWordSwapPuzzle(GRID3))
    }
}
