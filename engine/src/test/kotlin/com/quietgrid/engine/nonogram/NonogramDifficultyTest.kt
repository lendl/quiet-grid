package com.quietgrid.engine.nonogram

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NonogramDifficultyTest {
    @Test
    fun `buildNonogramClues finds run-lengths, defaulting to zero for a blank line`() {
        assertEquals(listOf(2, 1), buildNonogramClues(listOf(true, true, false, true)))
        assertEquals(listOf(0), buildNonogramClues(listOf(false, false, false)))
    }

    @Test
    fun `analyzeNonogramDifficulty solves a trivial fully-overlap-fillable 5x5 puzzle`() {
        val solution = listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)
        assertNotNull(metrics)
        assertEquals(15, metrics!!.filledCells)
    }

    @Test
    fun `classifyNonogramDifficulty returns easy for the trivial 5x5 puzzle above`() {
        val solution = listOf(
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
            listOf(true, true, true, true, true),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)!!
        assertEquals(Difficulty.EASY, classifyNonogramDifficulty(5, 5, metrics))
    }
}
