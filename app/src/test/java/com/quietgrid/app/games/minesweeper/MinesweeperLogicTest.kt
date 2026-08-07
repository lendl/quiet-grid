package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperLogicTest {

    @Test
    fun `createMinesweeperSession seeds an ungenerated board sized for the difficulty`() {
        val session = createMinesweeperSession(Difficulty.EASY)

        assertEquals(12, session.puzzle.rows)
        assertEquals(11, session.puzzle.cols)
        assertEquals(16, session.puzzle.mines)
        assertFalse(session.board.generated)
        assertEquals(MinesweeperStatus.PLAYING, session.board.status)
        assertTrue(session.board.cells.all { row -> row.all { it.state == MinesweeperCellState.HIDDEN } })
    }

    @Test
    fun `minesweeperScore is at its ceiling at zero seconds and floors out for very slow times`() {
        val instant = minesweeperScore(Difficulty.EASY, timeSeconds = 0)
        val verySlow = minesweeperScore(Difficulty.EASY, timeSeconds = 10_000)

        assertEquals(10_000, instant)
        assertEquals(1_000, verySlow)
    }

    @Test
    fun `minesweeperScore decreases as time increases`() {
        val early = minesweeperScore(Difficulty.MEDIUM, timeSeconds = 10)
        val later = minesweeperScore(Difficulty.MEDIUM, timeSeconds = 200)

        assertTrue(early > later)
    }

    @Test
    fun `hasMeaningfulProgress is false for a fresh ungenerated board`() {
        val session = createMinesweeperSession(Difficulty.EASY)

        assertFalse(minesweeperHasMeaningfulProgress(session))
    }

    @Test
    fun `hasMeaningfulProgress is true once the board has been generated`() {
        val session = createMinesweeperSession(Difficulty.EASY)
        val generated = session.copy(board = session.board.copy(generated = true))

        assertTrue(minesweeperHasMeaningfulProgress(generated))
    }

    @Test
    fun `hasMeaningfulProgress is true once any cell leaves the hidden state`() {
        val session = createMinesweeperSession(Difficulty.EASY)
        val flaggedCells = session.board.cells.mapIndexed { r, row ->
            row.mapIndexed { c, cell -> if (r == 0 && c == 0) cell.copy(state = MinesweeperCellState.FLAGGED) else cell }
        }
        val flagged = session.copy(board = session.board.copy(cells = flaggedCells))

        assertTrue(minesweeperHasMeaningfulProgress(flagged))
    }
}
