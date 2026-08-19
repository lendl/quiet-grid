// app/src/test/java/com/quietgrid/app/games/animaldoku/AnimalDokuLogicTest.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val TEST_PUZZLE = AnimalDokuPuzzleEntry(
    id = "test",
    size = 5,
    difficulty = "easy",
    regions = List(5) { row -> List(5) { row } },
    solution = listOf(0, 1, 2, 3, 4),
)

class AnimalDokuLogicTest {
    @Test
    fun `applyAnimalDokuTap marks an empty cell`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        val next = applyAnimalDokuTap(session, 0, 0)
        assertEquals(AnimalDokuCellState.MARKED, next?.cells?.get(0)?.get(0))
    }

    @Test
    fun `applyAnimalDokuTap unmarks a marked cell`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        val marked = applyAnimalDokuTap(session, 0, 0)!!
        val unmarked = applyAnimalDokuTap(marked, 0, 0)
        assertEquals(AnimalDokuCellState.EMPTY, unmarked?.cells?.get(0)?.get(0))
    }

    @Test
    fun `applyAnimalDokuTap is a no-op on a locked cell`() {
        val session = createAnimalDokuSession(TEST_PUZZLE).let {
            it.copy(cells = it.cells.mapIndexed { r, row -> row.mapIndexed { c, cell -> if (r == 0 && c == 0) AnimalDokuCellState.LOCKED_CORRECT else cell } })
        }
        assertNull(applyAnimalDokuTap(session, 0, 0))
    }

    @Test
    fun `applyAnimalDokuTap is a no-op once the game is no longer PLAYING`() {
        val session = createAnimalDokuSession(TEST_PUZZLE).copy(status = AnimalDokuStatus.WON)
        assertNull(applyAnimalDokuTap(session, 0, 0))
    }

    @Test
    fun `applyAnimalDokuDrag marks every visited cell when markAll is true`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        val next = applyAnimalDokuDrag(session, markAll = true, visited = listOf(0 to 0, 0 to 1, 0 to 2))
        assertEquals(AnimalDokuCellState.MARKED, next?.cells?.get(0)?.get(0))
        assertEquals(AnimalDokuCellState.MARKED, next?.cells?.get(0)?.get(1))
        assertEquals(AnimalDokuCellState.MARKED, next?.cells?.get(0)?.get(2))
    }

    @Test
    fun `applyAnimalDokuDrag unmarks every visited cell when markAll is false`() {
        val marked = createAnimalDokuSession(TEST_PUZZLE).let {
            it.copy(cells = it.cells.mapIndexed { r, row -> row.map { if (r == 0) AnimalDokuCellState.MARKED else it } })
        }
        val next = applyAnimalDokuDrag(marked, markAll = false, visited = listOf(0 to 0, 0 to 1))
        assertEquals(AnimalDokuCellState.EMPTY, next?.cells?.get(0)?.get(0))
        assertEquals(AnimalDokuCellState.EMPTY, next?.cells?.get(0)?.get(1))
    }

    @Test
    fun `applyAnimalDokuDrag skips locked cells among the visited set`() {
        val session = createAnimalDokuSession(TEST_PUZZLE).let {
            it.copy(cells = it.cells.mapIndexed { r, row -> row.mapIndexed { c, cell -> if (r == 0 && c == 1) AnimalDokuCellState.LOCKED_CORRECT else cell } })
        }
        val next = applyAnimalDokuDrag(session, markAll = true, visited = listOf(0 to 0, 0 to 1))
        assertEquals(AnimalDokuCellState.MARKED, next?.cells?.get(0)?.get(0))
        assertEquals(AnimalDokuCellState.LOCKED_CORRECT, next?.cells?.get(0)?.get(1))
    }

    @Test
    fun `applyAnimalDokuDrag direction stays fixed across repeated calls within the same gesture`() {
        // Regression test: onCellDrag fires once per newly-entered cell during a single continuous
        // swipe, each firing mutating the session. markAll must stay whatever the gesture started
        // with -- if it were re-derived from the (by-then-already-mutated) start cell's live state
        // on a later firing, the direction would flip mid-swipe. Simulates two firings of the same
        // gesture (markAll captured once, reused for both) and confirms the second firing keeps
        // marking rather than flipping to unmark just because the start cell became MARKED after
        // the first firing.
        val session = createAnimalDokuSession(TEST_PUZZLE)
        val afterFirstFiring = applyAnimalDokuDrag(session, markAll = true, visited = listOf(0 to 0, 0 to 1))
        checkNotNull(afterFirstFiring)
        assertEquals(AnimalDokuCellState.MARKED, afterFirstFiring.cells[0][0])

        val afterSecondFiring = applyAnimalDokuDrag(afterFirstFiring, markAll = true, visited = listOf(0 to 0, 0 to 1, 0 to 2))
        checkNotNull(afterSecondFiring)
        assertEquals(AnimalDokuCellState.MARKED, afterSecondFiring.cells[0][0])
        assertEquals(AnimalDokuCellState.MARKED, afterSecondFiring.cells[0][1])
        assertEquals(AnimalDokuCellState.MARKED, afterSecondFiring.cells[0][2])
    }

    @Test
    fun `applyAnimalDokuOpen locks a correct cell in and does not decrement lives`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        val result = applyAnimalDokuOpen(session, 0, 0)
        checkNotNull(result)
        assertTrue(result.wasCorrect)
        assertEquals(AnimalDokuCellState.LOCKED_CORRECT, result.session.cells[0][0])
        assertEquals(3, result.session.lives)
        assertEquals(AnimalDokuStatus.PLAYING, result.session.status)
    }

    @Test
    fun `applyAnimalDokuOpen locks an incorrect cell as wrong and decrements lives`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        // Solution row 0's animal is at column 0; opening column 1 is wrong.
        val result = applyAnimalDokuOpen(session, 0, 1)
        checkNotNull(result)
        assertFalse(result.wasCorrect)
        assertEquals(AnimalDokuCellState.LOCKED_WRONG, result.session.cells[0][1])
        assertEquals(2, result.session.lives)
    }

    @Test
    fun `applyAnimalDokuOpen ends the game as LOST after the third wrong open`() {
        var session = createAnimalDokuSession(TEST_PUZZLE)
        session = applyAnimalDokuOpen(session, 0, 1)!!.session
        session = applyAnimalDokuOpen(session, 1, 0)!!.session
        val result = applyAnimalDokuOpen(session, 2, 0)
        checkNotNull(result)
        assertEquals(0, result.session.lives)
        assertEquals(AnimalDokuStatus.LOST, result.session.status)
    }

    @Test
    fun `applyAnimalDokuOpen ends the game as WON once every row is correctly opened`() {
        var session = createAnimalDokuSession(TEST_PUZZLE)
        for (row in 0 until 4) session = applyAnimalDokuOpen(session, row, TEST_PUZZLE.solution[row])!!.session
        val result = applyAnimalDokuOpen(session, 4, TEST_PUZZLE.solution[4])
        checkNotNull(result)
        assertEquals(AnimalDokuStatus.WON, result.session.status)
    }

    @Test
    fun `applyAnimalDokuOpen is a no-op on an already-locked cell`() {
        val opened = applyAnimalDokuOpen(createAnimalDokuSession(TEST_PUZZLE), 0, 0)!!.session
        assertNull(applyAnimalDokuOpen(opened, 0, 0))
    }

    @Test
    fun `applyAnimalDokuOpen works directly on a marked cell without needing it cleared first`() {
        val session = applyAnimalDokuTap(createAnimalDokuSession(TEST_PUZZLE), 0, 0)!!
        val result = applyAnimalDokuOpen(session, 0, 0)
        checkNotNull(result)
        assertEquals(AnimalDokuCellState.LOCKED_CORRECT, result.session.cells[0][0])
    }
}
