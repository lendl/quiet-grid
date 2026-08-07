package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun cell(number: Int, row: Int, col: Int, hidden: Boolean = false) = ChimpTestCell(number, row, col, hidden)

private fun puzzle(gridSize: Int = 4, startCount: Int = 3, maxCount: Int = 5) =
    ChimpTestPuzzle(id = "p", difficulty = Difficulty.EASY.key, gridSize = gridSize, startCount = startCount, maxCount = maxCount)

private fun session(
    puzzle: ChimpTestPuzzle = puzzle(),
    currentCount: Int = puzzle.startCount,
    cells: List<ChimpTestCell>,
    nextExpected: Int = 1,
    revealAll: Boolean = false,
    roundTimes: List<Double> = emptyList(),
    roundStartElapsed: Double = 0.0,
    status: ChimpTestStatus = ChimpTestStatus.PLAYING,
) = ChimpTestSession(
    puzzle = puzzle,
    currentCount = currentCount,
    cells = cells,
    nextExpected = nextExpected,
    revealAll = revealAll,
    wrongTapCell = null,
    roundTimes = roundTimes,
    roundStartElapsed = roundStartElapsed,
    status = status,
)

class ChimpTestLogicTest {

    @Test
    fun `generateChimpTestCells assigns unique sequential numbers within grid bounds`() {
        val cells = generateChimpTestCells(count = 5, gridSize = 4)

        assertEquals(5, cells.size)
        assertEquals((1..5).toList(), cells.map { it.number }.sorted())
        assertTrue(cells.all { it.row in 0 until 4 && it.col in 0 until 4 })
        assertEquals(cells.size, cells.map { it.row to it.col }.distinct().size)
    }

    @Test
    fun `createChimpTestSession seeds from the difficulty config`() {
        val result = createChimpTestSession(Difficulty.EASY)

        assertEquals(4, result.puzzle.gridSize)
        assertEquals(3, result.puzzle.startCount)
        assertEquals(7, result.puzzle.maxCount)
        assertEquals(3, result.currentCount)
        assertEquals(3, result.cells.size)
        assertEquals(1, result.nextExpected)
        assertEquals(ChimpTestStatus.PLAYING, result.status)
    }

    @Test
    fun `tapping the correct first cell hides every cell and advances nextExpected`() {
        val s = session(cells = listOf(cell(1, 0, 0), cell(2, 1, 1), cell(3, 2, 2)))

        val result = runChimpTestAction(s, row = 0, col = 0, elapsedSeconds = 1.0)

        assertTrue(result.changed)
        assertTrue(result.effects.isEmpty())
        assertTrue(result.session.cells.all { it.hidden })
        assertEquals(2, result.session.nextExpected)
    }

    @Test
    fun `tapping the correct non-first cell only hides that cell`() {
        val s = session(
            cells = listOf(cell(1, 0, 0, hidden = true), cell(2, 1, 1), cell(3, 2, 2)),
            nextExpected = 2,
        )

        val result = runChimpTestAction(s, row = 1, col = 1, elapsedSeconds = 1.0)

        assertTrue(result.session.cells.first { it.number == 2 }.hidden)
        assertTrue(result.session.cells.first { it.number == 3 }.hidden.not())
        assertEquals(3, result.session.nextExpected)
    }

    @Test
    fun `tapping an empty location reveals all cells with a wrong-tap effect`() {
        val s = session(cells = listOf(cell(1, 0, 0), cell(2, 1, 1), cell(3, 2, 2)))

        val result = runChimpTestAction(s, row = 3, col = 3, elapsedSeconds = 1.0)

        assertTrue(result.changed)
        assertEquals(listOf(ChimpTestEffect.WrongTap), result.effects)
        assertTrue(result.session.revealAll)
        assertNull(result.session.wrongTapCell)
    }

    @Test
    fun `tapping a cell out of number order reveals all with the tapped number recorded`() {
        val s = session(cells = listOf(cell(1, 0, 0), cell(2, 1, 1), cell(3, 2, 2)), nextExpected = 1)

        val result = runChimpTestAction(s, row = 2, col = 2, elapsedSeconds = 1.0)

        assertEquals(listOf(ChimpTestEffect.WrongTap), result.effects)
        assertEquals(3, result.session.wrongTapCell)
    }

    @Test
    fun `tapping while revealAll is set is a no-op`() {
        val s = session(cells = listOf(cell(1, 0, 0)), revealAll = true)

        val result = runChimpTestAction(s, row = 0, col = 0, elapsedSeconds = 1.0)

        assertEquals(false, result.changed)
        assertEquals(s, result.session)
    }

    @Test
    fun `completing a round before maxCount advances to the next round with a fresh board`() {
        val s = session(
            puzzle = puzzle(startCount = 3, maxCount = 5),
            currentCount = 3,
            cells = listOf(cell(1, 0, 0, hidden = true), cell(2, 1, 1, hidden = true), cell(3, 2, 2)),
            nextExpected = 3,
            roundStartElapsed = 0.0,
        )

        val result = runChimpTestAction(s, row = 2, col = 2, elapsedSeconds = 4.0)

        assertEquals(ChimpTestStatus.PLAYING, result.session.status)
        assertEquals(4, result.session.currentCount)
        assertEquals(4, result.session.cells.size)
        assertEquals(1, result.session.nextExpected)
        assertEquals(listOf(4.0), result.session.roundTimes)
        assertEquals(4.0, result.session.roundStartElapsed, 0.0)
    }

    @Test
    fun `completing the final round at maxCount wins the puzzle`() {
        val s = session(
            puzzle = puzzle(startCount = 3, maxCount = 3),
            currentCount = 3,
            cells = listOf(cell(1, 0, 0, hidden = true), cell(2, 1, 1, hidden = true), cell(3, 2, 2)),
            nextExpected = 3,
            roundStartElapsed = 1.0,
        )

        val result = runChimpTestAction(s, row = 2, col = 2, elapsedSeconds = 3.5)

        assertEquals(ChimpTestStatus.WON, result.session.status)
        assertEquals(listOf(2.5), result.session.roundTimes)
    }

    @Test
    fun `chimpTestScore is zero unless the session is won`() {
        val playing = session(cells = emptyList(), status = ChimpTestStatus.PLAYING, roundTimes = listOf(1.0))

        assertEquals(0, chimpTestScore(playing))
    }

    @Test
    fun `chimpTestScore decreases with total round time but never below the floor`() {
        val fast = session(cells = emptyList(), status = ChimpTestStatus.WON, roundTimes = listOf(1.0, 1.0))
        val slow = session(cells = emptyList(), status = ChimpTestStatus.WON, roundTimes = listOf(50.0, 50.0))

        assertTrue(chimpTestScore(fast) > chimpTestScore(slow))
        assertEquals(1000, chimpTestScore(slow))
    }
}
