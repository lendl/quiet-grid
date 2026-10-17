package com.quietgrid.engine.minesweeper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperSolverTest {

    private fun solvable(board: TestLayout, row: Int, col: Int) =
        isMinesweeperSolvableWithoutGuessing(board.rows, board.cols, board.mines, board.index(row, col))

    @Test
    fun `single clues finish a board the opening leaves unfinished`() {
        val board = layout(
            ".*..",
            "....",
            "....",
        )

        assertTrue(solvable(board, 2, 3))
    }

    @Test
    fun `a fifty-fifty board needs a guess`() {
        val board = layout(
            "....",
            "*...",
        )

        assertFalse(solvable(board, 0, 3))
    }

    @Test
    fun `two overlapping clues finish a board single clues cannot`() {
        val board = layout(
            "*..*",
            "....",
            "....",
        )

        assertTrue(solvable(board, 2, 0))
    }

    @Test
    fun `the mines-left count clears tiles no clue touches`() {
        val board = layout(
            "...*.",
            "...*.",
        )

        assertTrue(solvable(board, 0, 0))
    }

    @Test
    fun `starting on a mine is never solvable`() {
        val board = layout(
            "*..",
            "...",
        )

        assertFalse(solvable(board, 0, 0))
    }
}
