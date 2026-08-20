package com.quietgrid.app.games.takuzu

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val solutionGrid = listOf(
    listOf(0, 1, 0, 1),
    listOf(1, 0, 1, 0),
    listOf(0, 1, 1, 0),
    listOf(1, 0, 0, 1),
)
private val maskGrid = listOf(
    listOf(false, false, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
)
private val puzzleEntry = TakuzuPuzzleEntry(id = "p", size = 4, difficulty = Difficulty.EASY.key, solution = gridToHex(solutionGrid), mask = maskToHex(maskGrid))

class TakuzuLogicTest {

    @Test
    fun `createTakuzuSession starts from the puzzle's mask, blanking only the ungiven cells`() {
        val session = createTakuzuSession(puzzleEntry)

        assertNull(session.board[0][0])
        assertNull(session.board[0][1])
        assertEquals(0, session.board[0][2])
        assertFalse(session.isGiven[0][0])
        assertTrue(session.isGiven[0][2])
        assertEquals(0, session.accuracyDrops)
    }

    @Test
    fun `pressing a given cell is rejected`() {
        val session = createTakuzuSession(puzzleEntry)

        assertNull(applyTakuzuPressCell(session, row = 0, col = 2))
    }

    @Test
    fun `pressing an ungiven cell cycles empty to 0 to 1 to empty`() {
        val session = createTakuzuSession(puzzleEntry)

        val first = applyTakuzuPressCell(session, row = 0, col = 0)!!
        assertEquals(0, first.board[0][0])

        val second = applyTakuzuPressCell(first, row = 0, col = 0)!!
        assertEquals(1, second.board[0][0])

        val third = applyTakuzuPressCell(second, row = 0, col = 0)!!
        assertNull(third.board[0][0])
    }

    @Test
    fun `pressing a finished cell is rejected`() {
        val session = createTakuzuSession(puzzleEntry).let { it.copy(finishedCells = it.finishedCells.mapIndexed { r, row -> row.mapIndexed { c, v -> v || (r == 0 && c == 0) } }) }

        assertNull(applyTakuzuPressCell(session, row = 0, col = 0))
    }

    @Test
    fun `finalizing a correct row marks its ungiven cells finished`() {
        val session = createTakuzuSession(puzzleEntry)
        val filledBoard = session.board.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0) solutionGrid[r][c] else v } }

        val result = applyTakuzuFinalizeValidation(session, filledBoard, listOf("r0"))

        assertEquals(listOf(0), result.effect.correctRowIndexes)
        assertTrue(result.session.finishedCells[0][0])
        assertTrue(result.session.finishedCells[0][1])
    }

    @Test
    fun `finalizing an incorrect row penalizes every line the wrong digits broke, once each`() {
        val session = createTakuzuSession(puzzleEntry)
        val wrongBoard = session.board.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0) listOf(1, 0, 0, 1)[c] else v } }

        val result = applyTakuzuFinalizeValidation(session, wrongBoard, listOf("r0"))

        assertEquals(listOf(0), result.effect.incorrectRowIndexes)
        assertEquals(3, result.session.accuracyDrops)

        val again = applyTakuzuFinalizeValidation(result.session, wrongBoard, listOf("r0"))
        assertEquals(3, again.session.accuracyDrops)
    }

    @Test
    fun `takuzuScore is at its ceiling immediately and floors out for very slow or inaccurate play`() {
        assertEquals(10_000, takuzuScore(Difficulty.EASY, timeSeconds = 0, accuracyDrops = 0))
        assertEquals(1_000, takuzuScore(Difficulty.EASY, timeSeconds = 100_000, accuracyDrops = 0))
        assertEquals(1_000, takuzuScore(Difficulty.EASY, timeSeconds = 0, accuracyDrops = 100))
    }

    @Test
    fun `takuzuAccuracyPct drops by 10 per accuracy drop down to zero`() {
        assertEquals(100, takuzuAccuracyPct(0))
        assertEquals(70, takuzuAccuracyPct(3))
        assertEquals(0, takuzuAccuracyPct(20))
    }

    @Test
    fun `hasMeaningfulProgress is false for a fresh session and true after any change`() {
        val session = createTakuzuSession(puzzleEntry)
        assertFalse(takuzuHasMeaningfulProgress(session))

        val pressed = applyTakuzuPressCell(session, row = 0, col = 0)!!
        assertTrue(takuzuHasMeaningfulProgress(pressed))
    }
}
