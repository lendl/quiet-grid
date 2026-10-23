package com.quietgrid.app.games.links

import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.extendLinksStroke
import com.quietgrid.engine.links.linksEndpointsOf
import com.quietgrid.engine.links.startLinksStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksLogicTest {
    private val solution = listOf(
        listOf(0, 1, 2),
        listOf(3, 6, 7, 8, 5, 4),
    )
    private val puzzle = LinksPuzzleEntry(
        id = "test",
        size = 3,
        difficulty = "easy",
        endpoints = linksEndpointsOf(solution),
        solution = solution,
    )

    private fun draw(session: LinksSession, vararg cells: Int): LinksSession {
        var stroke = startLinksStroke(session.puzzle, session.paths, cells.first())!!
        cells.drop(1).forEach { stroke = extendLinksStroke(session.puzzle, stroke, it) }
        return commitLinksStroke(session, stroke)
    }

    @Test
    fun `joining each pair once wins with full accuracy`() {
        var session = createLinksSession(puzzle)
        session = draw(session, 0, 1, 2)
        session = draw(session, 3, 6, 7, 8, 5, 4)
        assertEquals(LinksStatus.WON, session.status)
        assertEquals(2, session.moves)
        assertEquals(100, linksAccuracy(session))
    }

    @Test
    fun `redrawing the same color is free but switching back costs a move`() {
        var session = createLinksSession(puzzle)
        session = draw(session, 3, 6, 7)
        session = draw(session, 7, 8)
        assertEquals(1, session.moves)
        session = draw(session, 0, 1)
        session = draw(session, 8, 5, 4)
        assertEquals(3, session.moves)
    }

    @Test
    fun `a stroke that changes nothing leaves the session untouched`() {
        val session = createLinksSession(puzzle)
        val stroke = startLinksStroke(puzzle, session.paths, 0)!!
        assertSame(session, commitLinksStroke(session, stroke))
    }

    @Test
    fun `a hint draws the next pair and lowers accuracy`() {
        var session = createLinksSession(puzzle)
        session = applyLinksHint(session)!!
        assertEquals(listOf(0, 1, 2), session.paths[0])
        assertEquals(1, session.hintsUsed)
        session = draw(session, 3, 6, 7, 8, 5, 4)
        assertEquals(LinksStatus.WON, session.status)
        assertTrue(linksAccuracy(session) < 100)
        assertNull(applyLinksHint(session))
    }

    @Test
    fun `restoring rejects paths that overlap or jump`() {
        val session = createLinksSession(puzzle)
        assertTrue(isRestorableLinksSession(session))
        assertFalse(isRestorableLinksSession(session.copy(paths = listOf(listOf(0, 2), emptyList()))))
        assertFalse(isRestorableLinksSession(session.copy(paths = listOf(listOf(0, 1), listOf(1, 4)))))
    }

    @Test
    fun `score drops with hints, extra moves and time`() {
        val base = createLinksSession(puzzle).copy(moves = 2)
        assertEquals(990, linksScore(base, elapsedSeconds = 10))
        assertEquals(850, linksScore(base.copy(moves = 4, hintsUsed = 1), elapsedSeconds = 10))
    }
}
