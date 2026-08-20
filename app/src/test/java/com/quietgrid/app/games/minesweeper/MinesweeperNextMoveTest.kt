package com.quietgrid.app.games.minesweeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun cell(adjacentMines: Int = 0, state: MinesweeperCellState = MinesweeperCellState.HIDDEN) =
    MinesweeperCell(isMine = false, adjacentMines = adjacentMines, state = state)

private fun boardOf(rows: Int, cols: Int, cells: Map<Pair<Int, Int>, MinesweeperCell>, status: MinesweeperStatus = MinesweeperStatus.PLAYING) =
    MinesweeperBoard(
        rows = rows,
        cols = cols,
        mines = 0,
        generated = true,
        status = status,
        cells = List(rows) { r -> List(cols) { c -> cells[r to c] ?: cell(state = MinesweeperCellState.REVEALED, adjacentMines = 0) } },
    )

class MinesweeperNextMoveTest {

    @Test
    fun `a board that is not PLAYING falls back to a guess hint`() {
        val board = boardOf(1, 2, mapOf((0 to 0) to cell(1, MinesweeperCellState.REVEALED)), status = MinesweeperStatus.WON)

        assertEquals(MinesweeperGuessHint, getMinesweeperNextMoveHint(board))
    }

    @Test
    fun `a board with no revealed clues falls back to a guess hint`() {
        val board = boardOf(2, 2, emptyMap())

        assertEquals(MinesweeperGuessHint, getMinesweeperNextMoveHint(board))
    }

    @Test
    fun `a single clue fully explained by forced mines has nothing left to hint`() {
        val board = boardOf(
            rows = 1,
            cols = 2,
            cells = mapOf(
                (0 to 0) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
                (0 to 1) to cell(state = MinesweeperCellState.HIDDEN),
            ),
        )

        assertEquals(MinesweeperGuessHint, getMinesweeperNextMoveHint(board))
    }

    @Test
    fun `a shared hidden neighbor forced by one clue makes another clue's remaining neighbor safe`() {
        val board = boardOf(
            rows = 2,
            cols = 3,
            cells = mapOf(
                (0 to 0) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
                (0 to 1) to cell(state = MinesweeperCellState.HIDDEN),
                (0 to 2) to cell(adjacentMines = 0, state = MinesweeperCellState.REVEALED),
                (1 to 0) to cell(state = MinesweeperCellState.HIDDEN),
                (1 to 1) to cell(adjacentMines = 0, state = MinesweeperCellState.REVEALED),
                (1 to 2) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
            ),
        )

        val hint = getMinesweeperNextMoveHint(board)

        assertTrue(hint is MinesweeperPatternHint)
        val patternHint = hint as MinesweeperPatternHint
        assertEquals(MinesweeperHintPattern.GUARANTEED_SAFE_TILE, patternHint.pattern)
        assertEquals(listOf(1 to 0), patternHint.targetCells)
        assertEquals(0 to 0, patternHint.clueCell)
    }
}
