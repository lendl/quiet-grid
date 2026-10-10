package com.quietgrid.app.games.hashi

import com.quietgrid.engine.hashi.HashiBridge
import com.quietgrid.engine.hashi.HashiIsland
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HashiLogicTest {
    private val square = HashiPuzzleEntry(
        id = "square",
        rows = 3,
        cols = 3,
        difficulty = "easy",
        islands = listOf(HashiIsland(0, 0, 1), HashiIsland(0, 2, 1), HashiIsland(2, 0, 2), HashiIsland(2, 2, 2)),
        solution = listOf(HashiBridge(0, 2, 1), HashiBridge(1, 3, 1), HashiBridge(2, 3, 1)),
    )

    private val plus = HashiPuzzleEntry(
        id = "plus",
        rows = 5,
        cols = 5,
        difficulty = "easy",
        islands = listOf(HashiIsland(0, 2, 1), HashiIsland(2, 0, 1), HashiIsland(2, 4, 1), HashiIsland(4, 2, 1)),
        solution = emptyList(),
    )

    private fun tap(session: HashiSession, a: Int, b: Int): HashiSession {
        val outcome = applyHashiTap(session, session.board.edgeIndex(a, b))
        return (outcome as HashiTapOutcome.Changed).session
    }

    @Test
    fun `tapping a gap cycles through one, two and no bridges`() {
        var session = createHashiSession(square)
        val edge = session.board.edgeIndex(2, 3)
        session = tap(session, 2, 3)
        assertEquals(1, session.bridges[edge])
        session = tap(session, 2, 3)
        assertEquals(2, session.bridges[edge])
        session = tap(session, 2, 3)
        assertEquals(0, session.bridges[edge])
    }

    @Test
    fun `a bridge that would cross another is refused`() {
        var session = createHashiSession(plus)
        session = tap(session, 0, 3)
        assertEquals(HashiTapOutcome.Blocked, applyHashiTap(session, session.board.edgeIndex(1, 2)))
    }

    @Test
    fun `matching every island and joining them all wins`() {
        var session = createHashiSession(square)
        session = tap(session, 0, 2)
        session = tap(session, 1, 3)
        assertEquals(HashiStatus.PLAYING, session.status)
        session = tap(session, 2, 3)
        assertEquals(HashiStatus.WON, session.status)
        assertNull(applyHashiTap(session, 0))
    }

    @Test
    fun `islands report open, complete and overfull`() {
        var session = createHashiSession(square)
        session = tap(session, 0, 1)
        session = tap(session, 0, 2)
        val states = hashiIslandStates(session)
        assertEquals(HashiIslandState.OVERFULL, states[0])
        assertEquals(HashiIslandState.COMPLETE, states[1])
        assertEquals(HashiIslandState.OPEN, states[2])
    }

    @Test
    fun `a hint points at a wrong bridge before anything else`() {
        var session = createHashiSession(square)
        session = tap(session, 0, 1)
        val hinted = applyHashiHint(session)!!
        assertTrue(hinted.highlight!!.isMistake)
        assertEquals(session.board.edgeIndex(0, 1), hinted.highlight!!.edge)
        assertEquals(1, hinted.hintsUsed)
    }

    @Test
    fun `taps between islands resolve to that gap and taps on islands do not`() {
        val board = createHashiSession(square).board
        assertEquals(board.edgeIndex(0, 1), resolveHashiTap(board, tapRow = 0.5f, tapCol = 1.5f, toleranceCells = 0.5f))
        assertEquals(board.edgeIndex(0, 2), resolveHashiTap(board, tapRow = 1.5f, tapCol = 0.6f, toleranceCells = 0.5f))
        assertNull(resolveHashiTap(board, tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0.5f))
        assertNull(resolveHashiTap(board, tapRow = 1.5f, tapCol = 1.5f, toleranceCells = 0.5f))
    }
}
