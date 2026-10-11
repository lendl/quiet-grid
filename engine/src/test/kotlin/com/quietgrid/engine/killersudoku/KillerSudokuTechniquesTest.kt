package com.quietgrid.engine.killersudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KillerSudokuTechniquesTest {
    @Test
    fun `last cell in a cage takes the remaining sum`() {
        val layout = dominoLayout()
        val board = IntArray(81).also { it[0] = 5 }
        val move = CageLastCellTechnique.findMove(createKillerSolverState(layout, board))
        assertNotNull(move)
        assertTrue(move!!.isPlacement)
        assertEquals(SOLVED_GRID[move.placementCell!!], move.placementDigit)
    }

    @Test
    fun `cage combination removes digits no combination can use`() {
        val move = CageCombinationTechnique.findMove(createKillerSolverState(dominoLayout()))
        assertNotNull(move)
        assertEquals(KillerTechnique.CAGE_COMBINATION, move!!.technique)
        assertTrue(move.eliminations.isNotEmpty())
        assertTrue(move.eliminations.none { (cell, digit) -> SOLVED_GRID[cell] == digit })
    }

    @Test
    fun `innie from the rule of 45 places the cell left over in a row`() {
        val firstRow = (0 until 8).toList()
        val layout = layoutOf(listOf(firstRow, listOf(8, 17)) + (1 until 9).map { row -> (0 until 8).map { row * 9 + it } })
        val move = RuleOf45Technique(KillerTechnique.INNIE_OUTIE).findMove(createKillerSolverState(layout))
        assertNotNull(move)
        assertTrue(move!!.isPlacement)
        assertTrue(move.placementCell in listOf(8, 17))
        assertEquals(SOLVED_GRID[move.placementCell!!], move.placementDigit)
    }

    @Test
    fun `rule of 45 across two rows only uses cells sticking out of them`() {
        val layout = layoutOf(
            listOf(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
                listOf(9, 10, 11, 12, 13, 14, 15, 16),
                listOf(17, 26),
            ),
        )
        val state = createKillerSolverState(layout)
        val move = RuleOf45Technique(KillerTechnique.RULE_OF_45_MULTI_HOUSE).findMove(state)
        assertNotNull(move)
        assertEquals(KillerTechnique.RULE_OF_45_MULTI_HOUSE, move!!.technique)
        if (move.isPlacement) {
            assertEquals(SOLVED_GRID[move.placementCell!!], move.placementDigit)
        } else {
            assertTrue(move.eliminations.none { (cell, digit) -> SOLVED_GRID[cell] == digit })
        }
    }

    @Test
    fun `a required cage digit locked in one row is removed from the rest of that row`() {
        val layout = layoutOf(listOf(listOf(0, 1), listOf(2, 11)))
        val state = createKillerSolverState(layout)
        val move = CageHouseInteractionTechnique.findMove(state)
        if (move != null) assertTrue(move.eliminations.none { (cell, digit) -> SOLVED_GRID[cell] == digit })
    }
}
