package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeGeometryTest {
    @Test
    fun `computeCorridor stops at board edge`() {
        val corridor = computeCorridor(2, 3, ArrowDirection.UP, 10, 10)
        assertEquals(2, corridor.size)
        assertEquals(1, corridor[0].row)
        assertEquals(0, corridor[1].row)
    }

    @Test
    fun `computeCorridor is empty when head is already at the edge`() {
        val corridor = computeCorridor(0, 3, ArrowDirection.UP, 10, 10)
        assertTrue(corridor.isEmpty())
    }

    @Test
    fun `arrowDirectionFromKey round-trips with key`() {
        for (direction in ArrowDirection.entries) {
            assertEquals(direction, arrowDirectionFromKey(direction.key))
        }
    }

    @Test
    fun `ArrowEscapePieceData toPiece converts cells and direction`() {
        val data = ArrowEscapePieceData(cells = listOf(listOf(1, 2), listOf(1, 3)), headDirection = "right")
        val piece = data.toPiece()
        assertEquals(listOf(CellCoord(1, 2), CellCoord(1, 3)), piece.cells)
        assertEquals(ArrowDirection.RIGHT, piece.headDirection)
    }
}
