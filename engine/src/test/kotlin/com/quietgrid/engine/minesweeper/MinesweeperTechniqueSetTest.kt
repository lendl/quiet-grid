package com.quietgrid.engine.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperTechniqueSetTest {

    private fun stateWithCluesApplied(board: TestLayout, row: Int, col: Int): MinesweeperSolverState {
        val view = board.hiddenView().also { board.reveal(it, board.index(row, col)) }
        val state = MinesweeperSolverState(board.rows, board.cols, board.mineCount)
        for (cell in view.indices) if (view[cell] != MINESWEEPER_HIDDEN) state.open(cell, view[cell])
        for (deduction in nextMinesweeperDeductions(state)) deduction.mines.forEach(state::markMine)
        return state
    }

    private val minesLeftBoard = layout(
        "...*.",
        "...*.",
    )

    @Test
    fun `default technique set keeps today's ladder`() {
        val deductions = nextMinesweeperDeductions(stateWithCluesApplied(minesLeftBoard, 0, 0))

        assertEquals(1, deductions.size)
        assertEquals(MinesweeperTechnique.MINE_COUNT, deductions.single().technique)
    }

    @Test
    fun `leaving out the mine count stops at clue pairs`() {
        val deductions = nextMinesweeperDeductions(
            stateWithCluesApplied(minesLeftBoard, 0, 0),
            setOf(MinesweeperTechnique.CLUE_COUNT, MinesweeperTechnique.CLUE_PAIR),
        )

        assertTrue(deductions.isEmpty())
    }

    @Test
    fun `a locked safe cell is no longer unknown`() {
        val state = MinesweeperSolverState(3, 3, 1)
        state.open(4, 1)

        state.markSafeLocked(0)

        assertFalse(state.isUnknown(0))
        assertTrue(state.isSafeLocked(0))
        assertFalse(0 in state.unknownNeighbors(4))
        assertEquals(7, state.unknownCount)
    }
}
