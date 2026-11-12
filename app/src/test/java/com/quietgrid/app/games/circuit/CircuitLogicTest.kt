package com.quietgrid.app.games.circuit

import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP
import com.quietgrid.app.R
import com.quietgrid.engine.circuit.CircuitHint
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.CircuitTechnique
import com.quietgrid.engine.circuit.rotateCircuitMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitLogicTest {
    private val solution = listOf(
        CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_DOWN,
        CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN,
        CIRCUIT_UP, CIRCUIT_UP, CIRCUIT_UP,
    )
    private val puzzle = CircuitPuzzleEntry(
        id = "comb",
        rows = 3,
        cols = 3,
        difficulty = "easy",
        source = 4,
        solution = solution,
        start = solution.map { rotateCircuitMask(it, 3) },
    )

    private fun rotate(session: CircuitSession, cell: Int): CircuitSession =
        (applyCircuitRotate(session, cell) as CircuitTapOutcome.Changed).session

    @Test
    fun `tapping turns a tile a quarter clockwise and counts the move`() {
        val session = rotate(createCircuitSession(puzzle), 0)
        assertEquals(1, session.turns[0])
        assertEquals(1, session.moves)
        assertEquals(solution[0], session.masks[0])
    }

    @Test
    fun `a locked tile refuses to turn until it is unlocked`() {
        val locked = applyCircuitLockToggle(createCircuitSession(puzzle), 2)!!
        assertTrue(locked.locked[2])
        assertEquals(CircuitTapOutcome.Blocked, applyCircuitRotate(locked, 2))
        val unlocked = applyCircuitLockToggle(locked, 2)!!
        assertFalse(unlocked.locked[2])
        assertTrue(applyCircuitRotate(unlocked, 2) is CircuitTapOutcome.Changed)
    }

    @Test
    fun `turning every tile into place wins and lights the whole grid`() {
        var session = createCircuitSession(puzzle)
        for (cell in solution.indices) session = rotate(session, cell)
        assertEquals(CircuitStatus.WON, session.status)
        assertTrue(circuitLit(session).all { it })
        assertNull(applyCircuitRotate(session, 0))
    }

    @Test
    fun `a hint turns and locks one tile and charges once`() {
        val result = applyCircuitHint(createCircuitSession(puzzle))!!
        assertEquals(solution[result.hint.cell], result.session.masks[result.hint.cell])
        assertTrue(result.session.locked[result.hint.cell])
        assertEquals(1, result.session.hintsUsed)
        assertFalse(result.hint.isMistake)
    }

    @Test
    fun `a hint fixes a tile locked the wrong way and keeps it locked`() {
        val locked = applyCircuitLockToggle(createCircuitSession(puzzle), 5)!!
        val result = applyCircuitHint(locked)!!
        assertTrue(result.hint.isMistake)
        assertEquals(5, result.hint.cell)
        assertEquals(solution[5], result.session.masks[5])
        assertTrue(result.session.locked[5])
    }

    @Test
    fun `hint copy picks one body per technique and the mistake and fallback cases`() {
        assertEquals(R.string.circuit_hint_body_edge_match, circuitHintBodyRes(CircuitHint(0, 0, CircuitTechnique.EDGE_MATCH, false)))
        assertEquals(R.string.circuit_hint_body_no_loop, circuitHintBodyRes(CircuitHint(0, 0, CircuitTechnique.NO_LOOP, false)))
        assertEquals(R.string.circuit_hint_body_dead_end, circuitHintBodyRes(CircuitHint(0, 0, CircuitTechnique.DEAD_END, false)))
        assertEquals(R.string.circuit_hint_body_what_if, circuitHintBodyRes(CircuitHint(0, 0, CircuitTechnique.CONTRADICTION, false)))
        assertEquals(R.string.circuit_hint_body_mistake, circuitHintBodyRes(CircuitHint(0, 0, null, true)))
        assertEquals(R.string.circuit_hint_body_fallback, circuitHintBodyRes(CircuitHint(0, 0, null, false)))
        assertEquals(R.string.circuit_hint_title_mistake, circuitHintTitleRes(CircuitHint(0, 0, null, true)))
        assertEquals(R.string.circuit_hint_title_turn, circuitHintTitleRes(CircuitHint(0, 0, CircuitTechnique.EDGE_MATCH, false)))
    }

    @Test
    fun `taps map to the cell under the finger`() {
        assertEquals(4, resolveCircuitTap(3, 3, 1.5f, 1.2f))
        assertEquals(8, resolveCircuitTap(3, 3, 2.9f, 2.9f))
        assertNull(resolveCircuitTap(3, 3, 3.1f, 0.5f))
        assertNull(resolveCircuitTap(3, 3, -0.1f, 0.5f))
    }

    @Test
    fun `scores lose a point a second and a hundred per hint`() {
        assertEquals(1000, circuitScore(0, 0))
        assertEquals(780, circuitScore(2, 20))
        assertEquals(0, circuitScore(20, 0))
    }
}
