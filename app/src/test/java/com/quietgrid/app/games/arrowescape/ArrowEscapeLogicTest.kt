package com.quietgrid.app.games.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun twoPiecePuzzle() = ArrowEscapePuzzleEntry(
    id = "test",
    difficulty = "easy",
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

class ArrowEscapeLogicTest {
    @Test
    fun `a blocked attempt decrements one life, does not remove the piece, and records the selection`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val result = applyArrowEscapeAttempt(session, pieceIndex = 0)!!
        assertEquals(2, result.session.lives)
        assertTrue(result.session.removedIndices.isEmpty())
        assertEquals(0, result.session.selectedIndex)
        assertEquals(ArrowEscapeStatus.PLAYING, result.session.status)
        assertTrue(!result.removed)
    }

    @Test
    fun `a removable attempt clears the piece and can win`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val result = applyArrowEscapeAttempt(session, pieceIndex = 1)!!
        assertTrue(result.removed)
        assertEquals(setOf(1), result.session.removedIndices)
        assertEquals(ArrowEscapeStatus.PLAYING, result.session.status)

        val second = applyArrowEscapeAttempt(result.session, pieceIndex = 0)!!
        assertTrue(second.removed)
        assertEquals(ArrowEscapeStatus.WON, second.session.status)
    }

    @Test
    fun `zero lives after a blocked attempt ends the run as LOST`() {
        var session = createArrowEscapeSession(twoPiecePuzzle()).copy(lives = 1)
        val result = applyArrowEscapeAttempt(session, pieceIndex = 0)!!
        assertEquals(ArrowEscapeStatus.LOST, result.session.status)
        assertEquals(0, result.session.lives)
    }

    @Test
    fun `applyArrowEscapeHint selects the first currently-removable piece`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val hinted = applyArrowEscapeHint(session)!!
        assertEquals(1, hinted.selectedIndex)
    }

    @Test
    fun `applyArrowEscapeHint returns null when nothing is removable`() {
        val session = createArrowEscapeSession(twoPiecePuzzle()).copy(removedIndices = emptySet(), lives = 0, status = ArrowEscapeStatus.LOST)
        assertNull(applyArrowEscapeHint(session))
    }
}
