package com.quietgrid.engine.wordswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSwapSolverTest {

    @Test
    fun lengthSingleOpensWhenADirectionAndLengthHasOneSlot() {
        assertEquals(
            WordSwapDeduction(WordSwapTechnique.LENGTH_SINGLE, 0, 0),
            nextWordSwapDeduction(WordSwapSolverState.of(SPLIT4)),
        )
    }

    @Test
    fun onlySlotPlacesAWordThatFitsOneLine() {
        val state = WordSwapSolverState.of(SPLIT4).also { it.place(0, 0) }
        assertEquals(WordSwapDeduction(WordSwapTechnique.ONLY_SLOT, 3, 3), nextWordSwapDeduction(state))
    }

    @Test
    fun crossingSinglePlacesTheOnlyFittingWord() {
        val state = WordSwapSolverState.of(GRID3).also { it.place(0, 0) }
        assertEquals(WordSwapDeduction(WordSwapTechnique.CROSSING_SINGLE, 3, 3), nextWordSwapDeduction(state))
    }

    @Test
    fun crossCheckRulesOutClashingWords() {
        assertEquals(
            WordSwapDeduction(WordSwapTechnique.CROSS_CHECK, 0, 0),
            nextWordSwapDeduction(WordSwapSolverState.of(GRID3)),
        )
    }

    @Test
    fun placingAWrongWordMakesTheStateInfeasible() {
        assertFalse(WordSwapSolverState.of(GRID3).also { it.place(0, 1) }.isFeasible())
    }

    @Test
    fun copyDoesNotShareState() {
        val state = WordSwapSolverState.of(GRID3)
        state.copy().place(0, 0)
        assertFalse(state.isPlaced(0))
    }

    @Test
    fun solvePlacesEverySolutionWord() {
        val result = solveWordSwap(GRID3)
        assertTrue(result.solved)
        assertEquals(6, result.steps.size)
        result.steps.forEach { assertEquals(GRID3.words()[it.slot], it.word) }
    }

    @Test
    fun solveStopsOnABoardWithSeveralAnswers() {
        assertFalse(solveWordSwap(LATIN3).solved)
        assertEquals(2, countWordSwapArrangements(LATIN3.solution, 3, LATIN3.slots, LATIN3.words()))
    }

    @Test
    fun profileRecordsOpeningAndPlacements() {
        val profile = analyzeWordSwapSolveResult(solveWordSwap(GRID3))
        assertEquals(WordSwapTechnique.CROSS_CHECK, profile.opening)
        assertEquals(6, profile.placements)
        assertEquals(6, profile.counts.values.sum())
    }
}
