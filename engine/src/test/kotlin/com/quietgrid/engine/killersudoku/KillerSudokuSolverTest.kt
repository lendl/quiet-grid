package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KillerSudokuSolverTest {
    @Test
    fun `a layout of single-cell cages has exactly the source solution`() {
        val search = searchKillerSolutions(layoutOf(emptyList()))
        assertEquals(1, search.count)
        assertArrayEquals(SOLVED_GRID, search.solutions[0])
    }

    @Test
    fun `a layout of whole-row cages has more than one solution`() {
        val rows = (0 until 9).map { row -> (0 until 9).map { row * 9 + it } }
        val search = searchKillerSolutions(layoutOf(rows))
        assertEquals(2, search.count)
        assertFalse(search.exhausted)
    }

    @Test
    fun `every move in a trace agrees with the solution`() {
        val layout = dominoLayout()
        val trace = traceKillerSolve(layout)
        assertTrue(trace.moves.isNotEmpty())
        for (move in trace.moves) {
            if (move.isPlacement) {
                assertEquals(SOLVED_GRID[move.placementCell!!], move.placementDigit)
            } else {
                assertTrue(move.eliminations.none { (cell, digit) -> SOLVED_GRID[cell] == digit })
            }
        }
    }

    @Test
    fun `a fully solved trace reproduces the solution`() {
        val layout = layoutOf(listOf(0, 4).flatMap { row -> (0 until 8 step 2).map { listOf(row * 9 + it, row * 9 + it + 1) } })
        val trace = traceKillerSolve(layout)
        assertTrue(trace.solved)
        val state = createKillerSolverState(layout)
        trace.moves.forEach { applyKillerMove(state, it) }
        assertArrayEquals(SOLVED_GRID, state.grid.board)
    }

    @Test
    fun `the hint for an empty board places a correct digit`() {
        val hint = findKillerHint(dominoLayout(), IntArray(81))
        assertNotNull(hint)
        assertEquals(SOLVED_GRID[hint!!.cell], hint.digit)
        assertTrue(hint.path.last().isPlacement)
    }

    @Test
    fun `a cage that can no longer reach its sum is reported`() {
        val layout = dominoLayout()
        val board = IntArray(81).also { it[0] = 9 }
        assertEquals(layout.cageOfCell[0], findKillerCageConflict(layout, board))
        assertNull(findKillerCageConflict(layout, IntArray(81).also { it[0] = 5 }))
    }

    @Test
    fun `basic candidates respect the cage combinations`() {
        val masks = killerBasicCandidateMasks(dominoLayout(), IntArray(81))
        val digits = (1..9).filter { (masks[0] shr (it - 1)) and 1 == 1 }
        assertEquals(listOf(1, 2, 3, 5, 6, 7), digits)
    }

    @Test
    fun `difficulty follows the hardest technique and then the score`() {
        fun metrics(hardest: KillerTechnique?, score: Int) = KillerDifficultyMetrics(0, 0, 0, hardest, emptyMap(), 0, 0, score)
        assertEquals(Difficulty.EASY, classifyKillerDifficulty(metrics(KillerTechnique.CAGE_PERMUTATION, 0)))
        assertEquals(Difficulty.MEDIUM, classifyKillerDifficulty(metrics(KillerTechnique.RULE_OF_45, 120)))
        assertEquals(Difficulty.HARD, classifyKillerDifficulty(metrics(KillerTechnique.RULE_OF_45_MULTI_HOUSE, 120)))
        assertEquals(Difficulty.HARD, classifyKillerDifficulty(metrics(KillerTechnique.RULE_OF_45, 500)))
    }
}
