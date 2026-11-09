package com.quietgrid.engine.wordswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSwapHintTest {

    @Test
    fun hintNamesTheNextWordToPlace() {
        assertEquals(WordSwapHint.Placement(0, "abc", WordSwapTechnique.CROSS_CHECK), findWordSwapHint(GRID3, "bcaefdhig"))
    }

    @Test
    fun hintTreatsLitWordsAsPlaced() {
        assertEquals(WordSwapHint.Placement(3, "adg", WordSwapTechnique.CROSSING_SINGLE), findWordSwapHint(GRID3, "abcefdhig"))
    }

    @Test
    fun hintIgnoresUnlitPartialWords() {
        assertEquals(WordSwapHint.Placement(0, "abc", WordSwapTechnique.CROSS_CHECK), findWordSwapHint(GRID3, "abdcefhig"))
    }

    @Test
    fun hintFallsBackToASwapWhenStuck() {
        val board = LATIN3.solution.reversed()
        val hint = findWordSwapHint(LATIN3, board) as WordSwapHint.Swap
        val next = applyWordSwapMove(board, hint.move)!!
        assertEquals(wordSwapMinSwaps(board, LATIN3.solution) - 1, wordSwapMinSwaps(next, LATIN3.solution))
    }

    @Test
    fun hintIsNullOnASolvedBoard() {
        assertNull(findWordSwapHint(GRID3, GRID3.solution))
    }
}
