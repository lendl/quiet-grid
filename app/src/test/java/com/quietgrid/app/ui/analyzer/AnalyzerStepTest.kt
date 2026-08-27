package com.quietgrid.app.ui.analyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzerStepTest {

    @Test
    fun `replays every step from the initial board to solved, in order`() {
        val steps = replayAnalyzerSteps(
            initialBoard = 0,
            isSolved = { it == 3 },
            nextHint = { current -> if (current < 3) 1 else null },
            applyHint = { current, addend -> current + addend },
            maxSteps = 10,
        )

        assertEquals(3, steps.size)
        assertEquals(listOf(0, 1, 2), steps.map { it.boardBefore })
        assertEquals(listOf(1, 1, 1), steps.map { it.hint })
    }

    @Test
    fun `returns an empty list when the initial board is already solved`() {
        val steps = replayAnalyzerSteps(
            initialBoard = 3,
            isSolved = { it == 3 },
            nextHint = { 1 },
            applyHint = { current, addend -> current + addend },
            maxSteps = 10,
        )

        assertTrue(steps.isEmpty())
    }

    @Test
    fun `stops early when nextHint returns null before solved`() {
        val steps = replayAnalyzerSteps(
            initialBoard = 0,
            isSolved = { it == 100 },
            nextHint = { current -> if (current < 2) 1 else null },
            applyHint = { current, addend -> current + addend },
            maxSteps = 10,
        )

        assertEquals(2, steps.size)
        assertEquals(2, steps.last().let { it.boardBefore + it.hint })
    }

    @Test
    fun `stops at maxSteps even if nextHint would keep going`() {
        val steps = replayAnalyzerSteps(
            initialBoard = 0,
            isSolved = { false },
            nextHint = { 1 },
            applyHint = { current, addend -> current + addend },
            maxSteps = 5,
        )

        assertEquals(5, steps.size)
    }
}
