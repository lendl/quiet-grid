package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.minesweeper.MinesweeperDeduction
import com.quietgrid.engine.minesweeper.MinesweeperTechnique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun cell(adjacentMines: Int = 0, state: MinesweeperCellState = MinesweeperCellState.HIDDEN) =
    MinesweeperCell(isMine = false, adjacentMines = adjacentMines, state = state)

private fun boardOf(
    rows: Int,
    cols: Int,
    cells: Map<Pair<Int, Int>, MinesweeperCell>,
    status: MinesweeperStatus = MinesweeperStatus.PLAYING,
    mines: Int = 0,
) =
    MinesweeperBoard(
        rows = rows,
        cols = cols,
        mines = mines,
        generated = true,
        status = status,
        cells = List(rows) { r -> List(cols) { c -> cells[r to c] ?: cell(state = MinesweeperCellState.REVEALED, adjacentMines = 0) } },
    )

private fun wallOfOnesBoard() = boardOf(
    rows = 3,
    cols = 4,
    mines = 2,
    cells = mapOf(
        (0 to 0) to cell(),
        (0 to 1) to cell(),
        (0 to 2) to cell(),
        (0 to 3) to cell(),
        (1 to 0) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
        (1 to 1) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
        (1 to 2) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
        (1 to 3) to cell(adjacentMines = 1, state = MinesweeperCellState.REVEALED),
    ),
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

    @Test
    fun `when no clue pattern applies the mines-left count still points to safe tiles`() {
        val board = boardOf(
            rows = 2,
            cols = 5,
            mines = 2,
            cells = mapOf(
                (0 to 2) to cell(adjacentMines = 2, state = MinesweeperCellState.REVEALED),
                (1 to 2) to cell(adjacentMines = 2, state = MinesweeperCellState.REVEALED),
                (0 to 3) to cell(),
                (1 to 3) to cell(),
                (0 to 4) to cell(),
                (1 to 4) to cell(),
            ),
        )

        val hint = getMinesweeperNextMoveHint(board) as MinesweeperPatternHint

        assertEquals(MinesweeperHintPattern.MINES_LEFT, hint.pattern)
        assertEquals(listOf(0 to 4, 1 to 4), hint.targetCells)
    }

    @Test
    fun `an engine clue-pair deduction reads as an overlapping-clues hint`() {
        val board = wallOfOnesBoard()
        val deduction = MinesweeperDeduction(MinesweeperTechnique.CLUE_PAIR, clues = listOf(4, 5), safe = listOf(2), mines = emptyList())

        val hint = minesweeperHintFromDeduction(board, deduction)

        assertEquals(MinesweeperHintPattern.CLUE_PAIR, hint.pattern)
        assertEquals(1 to 0, hint.clueCell)
        assertEquals(1 to 1, hint.secondaryClueCell)
        assertEquals(listOf(0 to 2), hint.targetCells)
        assertEquals(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1), hint.evidenceCells)
    }

    @Test
    fun `an engine clue-count deduction reads as a full clue resolution hint`() {
        val board = wallOfOnesBoard()
        val deduction = MinesweeperDeduction(MinesweeperTechnique.CLUE_COUNT, clues = listOf(6), safe = listOf(1), mines = emptyList())

        val hint = minesweeperHintFromDeduction(board, deduction)

        assertEquals(MinesweeperHintPattern.FULL_CLUE_RESOLUTION, hint.pattern)
        assertEquals(1 to 2, hint.clueCell)
        assertEquals(1, hint.mineCount)
        assertEquals(listOf(0 to 1), hint.targetCells)
    }

    @Test
    fun `hints never ask for a guess while playing a freshly dealt board`() {
        for (difficulty in listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)) repeat(2) {
            val puzzle = createMinesweeperPuzzle(difficulty)
            var board = revealMinesweeperCell(createMinesweeperBoard(puzzle), puzzle, puzzle.rows / 2, puzzle.cols / 2)
            while (board.status == MinesweeperStatus.PLAYING) {
                val hint = getMinesweeperNextMoveHint(board)
                assertTrue("$difficulty hint asked for a guess", hint is MinesweeperPatternHint)
                for ((row, col) in hint.targetCells) board = revealMinesweeperCell(board, puzzle, row, col)
                assertNotEquals("$difficulty hint led onto a mine", MinesweeperStatus.LOST, board.status)
            }
        }
    }
}
