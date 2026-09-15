package com.quietgrid.app.games.starbattle

import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val TEST_PUZZLE_K1 = StarBattlePuzzleEntry(
    id = "test-k1",
    size = 3,
    difficulty = "easy",
    k = 1,
    regions = List(3) { row -> List(3) { row } },
    solution = listOf(listOf(0), listOf(1), listOf(2)),
)

private val TEST_PUZZLE_K2 = StarBattlePuzzleEntry(
    id = "test-k2",
    size = 3,
    difficulty = "medium",
    k = 2,
    regions = List(3) { row -> List(3) { row } },
    solution = listOf(listOf(0, 1), listOf(1, 2), listOf(0, 2)),
)

class StarBattleLogicTest {
    @Test
    fun `applyStarBattleTap marks an empty cell`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1)
        val next = applyStarBattleTap(session, 0, 0)
        assertEquals(StarBattleCellState.MARKED, next?.cells?.get(0)?.get(0))
    }

    @Test
    fun `applyStarBattleTap unmarks a marked cell`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1)
        val marked = applyStarBattleTap(session, 0, 0)!!
        val unmarked = applyStarBattleTap(marked, 0, 0)
        assertEquals(StarBattleCellState.EMPTY, unmarked?.cells?.get(0)?.get(0))
    }

    @Test
    fun `applyStarBattleTap is a no-op on a locked cell`() {
        val opened = applyStarBattleOpen(createStarBattleSession(TEST_PUZZLE_K1), 0, 0)!!.session
        assertNull(applyStarBattleTap(opened, 0, 0))
    }

    @Test
    fun `applyStarBattleTap is a no-op once the game is no longer PLAYING`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1).copy(status = StarBattleStatus.WON)
        assertNull(applyStarBattleTap(session, 0, 0))
    }

    @Test
    fun `applyStarBattleDrag marks every visited cell when markAll is true`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1)
        val next = applyStarBattleDrag(session, markAll = true, visited = listOf(0 to 0, 0 to 1, 0 to 2))
        assertEquals(StarBattleCellState.MARKED, next?.cells?.get(0)?.get(0))
        assertEquals(StarBattleCellState.MARKED, next?.cells?.get(0)?.get(1))
        assertEquals(StarBattleCellState.MARKED, next?.cells?.get(0)?.get(2))
    }

    @Test
    fun `applyStarBattleDrag unmarks every visited cell when markAll is false`() {
        val marked = createStarBattleSession(TEST_PUZZLE_K1).let {
            it.copy(cells = it.cells.mapIndexed { r, row -> row.map { if (r == 0) StarBattleCellState.MARKED else it } })
        }
        val next = applyStarBattleDrag(marked, markAll = false, visited = listOf(0 to 0, 0 to 1))
        assertEquals(StarBattleCellState.EMPTY, next?.cells?.get(0)?.get(0))
        assertEquals(StarBattleCellState.EMPTY, next?.cells?.get(0)?.get(1))
    }

    @Test
    fun `applyStarBattleDrag skips locked cells among the visited set`() {
        val session = applyStarBattleOpen(createStarBattleSession(TEST_PUZZLE_K1), 0, 1)!!.session
        val next = applyStarBattleDrag(session, markAll = true, visited = listOf(0 to 0, 0 to 1))
        assertEquals(StarBattleCellState.MARKED, next?.cells?.get(0)?.get(0))
        assertEquals(StarBattleCellState.LOCKED_WRONG, next?.cells?.get(0)?.get(1))
    }

    @Test
    fun `applyStarBattleOpen locks a correct cell in and does not decrement lives`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1)
        val result = applyStarBattleOpen(session, 0, 0)
        checkNotNull(result)
        assertTrue(result.wasCorrect)
        assertEquals(StarBattleCellState.LOCKED_CORRECT, result.session.cells[0][0])
        assertEquals(3, result.session.lives)
        assertEquals(StarBattleStatus.PLAYING, result.session.status)
    }

    @Test
    fun `applyStarBattleOpen locks an incorrect cell as wrong and decrements lives`() {
        val session = createStarBattleSession(TEST_PUZZLE_K1)
        val result = applyStarBattleOpen(session, 0, 1)
        checkNotNull(result)
        assertFalse(result.wasCorrect)
        assertEquals(StarBattleCellState.LOCKED_WRONG, result.session.cells[0][1])
        assertEquals(2, result.session.lives)
    }

    @Test
    fun `applyStarBattleOpen ends the game as LOST after the third wrong open`() {
        var session = createStarBattleSession(TEST_PUZZLE_K1)
        session = applyStarBattleOpen(session, 0, 1)!!.session
        session = applyStarBattleOpen(session, 1, 0)!!.session
        val result = applyStarBattleOpen(session, 2, 0)
        checkNotNull(result)
        assertEquals(0, result.session.lives)
        assertEquals(StarBattleStatus.LOST, result.session.status)
    }

    @Test
    fun `applyStarBattleOpen is a no-op on an already-locked cell`() {
        val opened = applyStarBattleOpen(createStarBattleSession(TEST_PUZZLE_K1), 0, 0)!!.session
        assertNull(applyStarBattleOpen(opened, 0, 0))
    }

    @Test
    fun `applyStarBattleOpen works directly on a marked cell without needing it cleared first`() {
        val session = applyStarBattleTap(createStarBattleSession(TEST_PUZZLE_K1), 0, 0)!!
        val result = applyStarBattleOpen(session, 0, 0)
        checkNotNull(result)
        assertEquals(StarBattleCellState.LOCKED_CORRECT, result.session.cells[0][0])
    }

    @Test
    fun `applyStarBattleOpen treats either solution column in a K2 row as correct via set membership`() {
        val session = createStarBattleSession(TEST_PUZZLE_K2)

        val openedFirstColumn = applyStarBattleOpen(session, 0, 0)
        checkNotNull(openedFirstColumn)
        assertTrue(openedFirstColumn.wasCorrect)
        assertEquals(StarBattleCellState.LOCKED_CORRECT, openedFirstColumn.session.cells[0][0])

        val openedSecondColumn = applyStarBattleOpen(session, 0, 1)
        checkNotNull(openedSecondColumn)
        assertTrue(openedSecondColumn.wasCorrect)
        assertEquals(StarBattleCellState.LOCKED_CORRECT, openedSecondColumn.session.cells[0][1])
    }

    @Test
    fun `applyStarBattleOpen treats a column outside the K2 solution set as wrong`() {
        val session = createStarBattleSession(TEST_PUZZLE_K2)
        val result = applyStarBattleOpen(session, 0, 2)
        checkNotNull(result)
        assertFalse(result.wasCorrect)
        assertEquals(StarBattleCellState.LOCKED_WRONG, result.session.cells[0][2])
        assertEquals(2, result.session.lives)
    }

    @Test
    fun `applyStarBattleOpen only reaches WON once every row's full K2 quota of correct cells is opened`() {
        var session = createStarBattleSession(TEST_PUZZLE_K2)

        val firstColumnPerRow = listOf(Triple(0, 0, 0), Triple(1, 1, 1), Triple(2, 2, 0))
        for ((row, _, col) in firstColumnPerRow) {
            val result = applyStarBattleOpen(session, row, col)
            checkNotNull(result)
            assertTrue(result.wasCorrect)
            session = result.session
        }
        assertEquals(StarBattleStatus.PLAYING, session.status)

        val secondColumnPerRow = listOf(0 to 1, 1 to 2, 2 to 2)
        for ((index, pair) in secondColumnPerRow.withIndex()) {
            val (row, col) = pair
            val result = applyStarBattleOpen(session, row, col)
            checkNotNull(result)
            assertTrue(result.wasCorrect)
            session = result.session
            if (index < secondColumnPerRow.lastIndex) {
                assertEquals(StarBattleStatus.PLAYING, session.status)
            }
        }
        assertEquals(StarBattleStatus.WON, session.status)
    }
}
