package com.quietgrid.engine.circuit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitBoardTest {
    private val corner = CIRCUIT_UP or CIRCUIT_RIGHT
    private val tee = CIRCUIT_UP or CIRCUIT_RIGHT or CIRCUIT_DOWN

    @Test
    fun `rotating turns arms clockwise`() {
        assertEquals(CIRCUIT_RIGHT, rotateCircuitMask(CIRCUIT_UP))
        assertEquals(CIRCUIT_UP, rotateCircuitMask(CIRCUIT_LEFT))
        assertEquals(CIRCUIT_RIGHT or CIRCUIT_DOWN, rotateCircuitMask(corner))
        assertEquals(corner, rotateCircuitMask(corner, 4))
        assertEquals(rotateCircuitMask(corner, 3), rotateCircuitMask(corner, -1))
        assertEquals(CIRCUIT_DOWN, oppositeCircuitDirection(CIRCUIT_UP))
    }

    @Test
    fun `straights have two orientations and the rest four`() {
        assertEquals(2, circuitOrientations(CIRCUIT_UP or CIRCUIT_DOWN).size)
        assertEquals(4, circuitOrientations(corner).size)
        assertEquals(4, circuitOrientations(tee).size)
        assertEquals(4, circuitOrientations(CIRCUIT_LEFT).size)
        assertEquals(2, circuitTurnsBetween(CIRCUIT_UP, CIRCUIT_DOWN))
    }

    @Test
    fun `neighbors stop at the edge unless the board wraps`() {
        val flat = CircuitBoard(3, 4, 5)
        assertEquals(-1, flat.neighbor(0, CIRCUIT_UP))
        assertEquals(-1, flat.neighbor(3, CIRCUIT_RIGHT))
        assertEquals(4, flat.neighbor(0, CIRCUIT_DOWN))
        val torus = CircuitBoard(3, 4, 5, wrap = true)
        assertEquals(8, torus.neighbor(0, CIRCUIT_UP))
        assertEquals(0, torus.neighbor(3, CIRCUIT_RIGHT))
        assertTrue(torus.wrapsAcross(3, CIRCUIT_RIGHT))
        assertFalse(torus.wrapsAcross(2, CIRCUIT_RIGHT))
    }

    @Test
    fun `a connected tree with every arm matched is solved`() {
        val board = CircuitBoard(2, 2, 0)
        val solved = intArrayOf(CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT, CIRCUIT_UP or CIRCUIT_RIGHT, CIRCUIT_LEFT)
        assertTrue(isCircuitSolved(board, solved))
        assertTrue(circuitLitCells(board, solved).all { it })
        val broken = solved.copyOf().also { it[1] = CIRCUIT_DOWN }
        assertFalse(isCircuitSolved(board, broken))
        assertArrayEquals(booleanArrayOf(true, false, true, true), circuitLitCells(board, broken))
    }

    @Test
    fun `a closed loop is not solved even when every arm matches`() {
        val board = CircuitBoard(2, 2, 0)
        val loop = intArrayOf(CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_RIGHT, CIRCUIT_LEFT or CIRCUIT_UP)
        assertFalse(isCircuitSolved(board, loop))
    }
}
