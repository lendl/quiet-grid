package com.quietgrid.app.games.flowfree

import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreeLogicTest {
    private val puzzle = FlowFreePuzzleEntry(
        id = "test",
        size = 3,
        difficulty = "easy",
        pairCount = 2,
        endpoints = listOf(
            listOf(0, -1, -1),
            listOf(0, -1, -1),
            listOf(1, -1, 1),
        ),
        paths = listOf(listOf(0, 1, 2, 5, 4, 3), listOf(6, 7, 8)),
    )

    @Test
    fun `starting a drag on a non-endpoint cell does nothing`() {
        val session = createFlowFreeSession(puzzle)
        val result = flowFreeStartDrag(session, 0, 1)
        assertEquals(session, result)
    }

    @Test
    fun `starting a drag on an endpoint begins a one-cell path for that color`() {
        val session = flowFreeStartDrag(createFlowFreeSession(puzzle), 0, 0)
        assertEquals(listOf(0 to 0), session.paths[0])
        assertEquals(0, session.activeColor)
    }

    @Test
    fun `extending into an orthogonally adjacent empty cell grows the path`() {
        var session = flowFreeStartDrag(createFlowFreeSession(puzzle), 0, 0)
        session = flowFreeExtendDrag(session, 0, 1)
        assertEquals(listOf(0 to 0, 0 to 1), session.paths[0])
    }

    @Test
    fun `extending into a non-adjacent cell is ignored`() {
        var session = flowFreeStartDrag(createFlowFreeSession(puzzle), 0, 0)
        session = flowFreeExtendDrag(session, 0, 2)
        assertEquals(listOf(0 to 0), session.paths[0])
    }

    @Test
    fun `extending back onto an already-visited cell in the same path truncates it`() {
        var session = flowFreeStartDrag(createFlowFreeSession(puzzle), 0, 0)
        session = flowFreeExtendDrag(session, 0, 1)
        session = flowFreeExtendDrag(session, 0, 0)
        assertEquals(listOf(0 to 0), session.paths[0])
    }

    @Test
    fun `extending onto a cell already owned by another color is ignored`() {
        var session = flowFreeStartDrag(createFlowFreeSession(puzzle), 2, 0)
        session = session.copy(paths = session.paths + (0 to listOf(1 to 1)))
        session = flowFreeExtendDrag(session, 2, 1)
        session = flowFreeExtendDrag(session, 1, 1)
        assertEquals(listOf(2 to 0, 2 to 1), session.paths[1])
    }

    @Test
    fun `flowFreeIsSolved is false when cells remain uncovered`() {
        val session = flowFreeStartDrag(createFlowFreeSession(puzzle), 0, 0)
        assertFalse(flowFreeIsSolved(session))
    }

    @Test
    fun `flowFreeIsSolved is true once every color connects both endpoints and covers the board`() {
        var session = createFlowFreeSession(puzzle)
        session = session.copy(
            paths = mapOf(
                0 to listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 1, 1 to 0),
                1 to listOf(2 to 0, 2 to 1, 2 to 2),
            ),
        )
        assertTrue(flowFreeIsSolved(session))
    }
}
