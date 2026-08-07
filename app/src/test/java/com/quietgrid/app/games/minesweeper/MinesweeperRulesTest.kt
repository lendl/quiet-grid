package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun cell(isMine: Boolean = false, adjacentMines: Int = 0, state: MinesweeperCellState = MinesweeperCellState.HIDDEN) =
    MinesweeperCell(isMine, adjacentMines, state)

/** A 3x3 board with a single mine at (0,0); every other cell has 0 adjacent mines, so revealing
 * any safe cell cascades to reveal the entire non-mine area in one move. Board is pre-generated
 * so reveal/flag tests never touch the randomized generator. */
private fun threeByThreeSingleMineBoard(): MinesweeperBoard {
    val cells = List(3) { r -> List(3) { c -> if (r == 0 && c == 0) cell(isMine = true) else cell() } }
        .mapIndexed { r, row -> row.mapIndexed { c, cellValue -> cellValue.copy(adjacentMines = if (r <= 1 && c <= 1 && !(r == 0 && c == 0)) 1 else 0) } }
    return MinesweeperBoard(rows = 3, cols = 3, mines = 1, generated = true, cells = cells, status = MinesweeperStatus.PLAYING)
}

private val puzzle = MinesweeperPuzzle(difficulty = Difficulty.EASY.key, profileId = "test", rows = 3, cols = 3, mines = 1)

class MinesweeperRulesTest {

    @Test
    fun `createMinesweeperBoard is ungenerated with every cell hidden and non-mine`() {
        val board = createMinesweeperBoard(puzzle)

        assertEquals(false, board.generated)
        assertTrue(board.cells.all { row -> row.all { it.state == MinesweeperCellState.HIDDEN && !it.isMine } })
    }

    @Test
    fun `revealing an out-of-bounds cell leaves the board unchanged`() {
        val board = threeByThreeSingleMineBoard()

        val result = revealMinesweeperCell(board, puzzle, row = -1, col = 0)

        assertEquals(board, result)
    }

    @Test
    fun `revealing on a board that is not PLAYING is a no-op`() {
        val board = threeByThreeSingleMineBoard().copy(status = MinesweeperStatus.LOST)

        val result = revealMinesweeperCell(board, puzzle, row = 1, col = 1)

        assertEquals(board, result)
    }

    @Test
    fun `revealing a flagged cell does not reveal it`() {
        val flaggedCells = threeByThreeSingleMineBoard().cells.mapIndexed { r, row ->
            row.mapIndexed { c, cellValue -> if (r == 1 && c == 1) cellValue.copy(state = MinesweeperCellState.FLAGGED) else cellValue }
        }
        val board = threeByThreeSingleMineBoard().copy(cells = flaggedCells)

        val result = revealMinesweeperCell(board, puzzle, row = 1, col = 1)

        assertEquals(MinesweeperCellState.FLAGGED, result.cells[1][1].state)
    }

    @Test
    fun `revealing the mine detonates it and reveals every mine as a loss`() {
        val board = threeByThreeSingleMineBoard()

        val result = revealMinesweeperCell(board, puzzle, row = 0, col = 0)

        assertEquals(MinesweeperStatus.LOST, result.status)
        assertEquals(MinesweeperCellState.REVEALED, result.cells[0][0].state)
    }

    @Test
    fun `revealing a zero-adjacent safe cell cascades and wins when only the mine remains hidden`() {
        val board = threeByThreeSingleMineBoard()

        val result = revealMinesweeperCell(board, puzzle, row = 2, col = 2)

        assertEquals(MinesweeperStatus.WON, result.status)
        assertEquals(MinesweeperCellState.HIDDEN, result.cells[0][0].state)
        for (r in 0 until 3) for (c in 0 until 3) {
            if (r == 0 && c == 0) continue
            assertEquals("cell ($r,$c)", MinesweeperCellState.REVEALED, result.cells[r][c].state)
        }
    }

    @Test
    fun `toggling a flag on a hidden cell flags it, and again unflags it`() {
        val board = threeByThreeSingleMineBoard()

        val flagged = toggleMinesweeperFlag(board, row = 1, col = 1)
        assertEquals(MinesweeperCellState.FLAGGED, flagged.cells[1][1].state)

        val unflagged = toggleMinesweeperFlag(flagged, row = 1, col = 1)
        assertEquals(MinesweeperCellState.HIDDEN, unflagged.cells[1][1].state)
    }

    @Test
    fun `toggling a flag on a revealed cell is a no-op`() {
        val board = revealMinesweeperCell(threeByThreeSingleMineBoard(), puzzle, row = 2, col = 2)

        val result = toggleMinesweeperFlag(board, row = 2, col = 2)

        assertEquals(board, result)
    }

    @Test
    fun `toggling a flag before the board is generated still flags the cell`() {
        val board = threeByThreeSingleMineBoard().copy(generated = false)

        val result = toggleMinesweeperFlag(board, row = 1, col = 1)

        assertEquals(MinesweeperCellState.FLAGGED, result.cells[1][1].state)
    }

    @Test
    fun `countFlaggedMinesweeperCells counts only flagged cells`() {
        val board = toggleMinesweeperFlag(toggleMinesweeperFlag(threeByThreeSingleMineBoard(), 0, 1), 1, 0)

        assertEquals(2, countFlaggedMinesweeperCells(board))
    }
}
