package com.quietgrid.engine.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MinesweeperDeductionTest {

    private fun openedView(board: TestLayout, row: Int, col: Int): IntArray =
        board.hiddenView().also { board.reveal(it, board.index(row, col)) }

    private fun deduce(board: TestLayout, view: IntArray) =
        findMinesweeperSafeDeduction(board.rows, board.cols, board.mineCount, view)

    @Test
    fun `a clue whose mines are all known clears its other tiles`() {
        val board = layout(
            ".*..",
            "....",
            "....",
        )

        val deduction = deduce(board, openedView(board, 2, 3))!!

        assertEquals(MinesweeperTechnique.CLUE_COUNT, deduction.technique)
        assertEquals(listOf(board.index(1, 0)), deduction.clues)
        assertEquals(listOf(board.index(0, 0)), deduction.safe)
    }

    @Test
    fun `two overlapping clues read together clear a tile`() {
        val board = layout(
            "*..*",
            "....",
            "....",
        )

        val deduction = deduce(board, openedView(board, 2, 0))!!

        assertEquals(MinesweeperTechnique.CLUE_PAIR, deduction.technique)
        assertEquals(listOf(board.index(1, 0), board.index(1, 1)), deduction.clues)
        assertEquals(listOf(board.index(0, 2)), deduction.safe)
    }

    @Test
    fun `the mines-left count clears tiles once every mine is pinned down`() {
        val board = layout(
            "...*.",
            "...*.",
        )

        val deduction = deduce(board, openedView(board, 0, 0))!!

        assertEquals(MinesweeperTechnique.MINE_COUNT, deduction.technique)
        assertTrue(deduction.clues.isEmpty())
        assertEquals(listOf(board.index(0, 4), board.index(1, 4)), deduction.safe)
    }

    @Test
    fun `a fifty-fifty has no safe deduction`() {
        val board = layout(
            "....",
            "*...",
        )

        assertNull(deduce(board, openedView(board, 0, 3)))
    }

    @Test
    fun `a fully cleared board has no safe deduction`() {
        val board = layout(
            "*..",
            "...",
        )
        val view = board.hiddenView()
        for (cell in 0 until board.size) board.reveal(view, cell)

        assertNull(deduce(board, view))
    }

    @Test
    fun `every reachable position of a no-guess board still has a safe deduction`() {
        val random = Random(17)
        var boardsChecked = 0
        repeat(300) {
            val start = random.nextInt(11 * 18)
            val board = randomLayout(18, 11, 42, start, random)
            if (!isMinesweeperSolvableWithoutGuessing(board.rows, board.cols, board.mines, start)) return@repeat
            boardsChecked++
            val view = board.hiddenView()
            board.reveal(view, start)
            while (!board.isCleared(view)) {
                val deduction = deduce(board, view)
                assertNotNull("board $boardsChecked ran out of deductions", deduction)
                for (cell in deduction!!.safe) {
                    assertFalse("board $boardsChecked deduced a mine as safe", board.mines[cell])
                    board.reveal(view, cell)
                }
                if (random.nextInt(4) == 0) {
                    val hiddenSafe = (0 until board.size).filter { !board.mines[it] && view[it] == MINESWEEPER_HIDDEN }
                    if (hiddenSafe.isNotEmpty()) board.reveal(view, hiddenSafe.random(random))
                }
            }
        }
        assertTrue("only $boardsChecked no-guess boards found", boardsChecked >= 20)
    }
}
