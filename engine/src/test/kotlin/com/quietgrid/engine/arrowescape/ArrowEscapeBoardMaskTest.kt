package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeBoardMaskTest {
    private val gapped = ArrowEscapePuzzleEntry("x", "easy", 1, 6, emptyList(), "gap", listOf("##..##"))

    @Test
    fun `entries without a mask treat every cell as active`() {
        val entry = ArrowEscapePuzzleEntry("x", "easy", 3, 3, emptyList())
        assertTrue(entry.isActiveCell(2, 2))
        assertFalse(entry.isActiveCell(3, 0))
    }

    @Test
    fun `mask rows mark active and void cells`() {
        assertTrue(gapped.isActiveCell(0, 1))
        assertFalse(gapped.isActiveCell(0, 2))
        assertTrue(gapped.isActiveCell(0, 4))
        assertFalse(gapped.isActiveCell(0, 6))
    }

    @Test
    fun `an arrow across a void gap is still blocked by the arrow beyond it`() {
        val pieces = listOf(
            ArrowEscapePiece(listOf(CellCoord(0, 0), CellCoord(0, 1)), ArrowDirection.RIGHT),
            ArrowEscapePiece(listOf(CellCoord(0, 4), CellCoord(0, 5)), ArrowDirection.RIGHT),
        )
        val owners = buildCellOwnerMap(pieces)
        assertFalse(isPieceRemovable(0, pieces, emptySet(), owners, rows = 1, cols = 6))
        assertTrue(isPieceRemovable(0, pieces, setOf(1), owners, rows = 1, cols = 6))
    }

    @Test
    fun `a corridor crossing only void cells counts as a border exit`() {
        val pieces = listOf(ArrowEscapePiece(listOf(CellCoord(0, 0), CellCoord(0, 1)), ArrowDirection.RIGHT))
        val active = setOf(CellCoord(0, 0), CellCoord(0, 1))
        assertEquals(0.0, profileArrowEscapePuzzle(pieces, 1, 4).borderExitShare, 1e-9)
        assertEquals(1.0, profileArrowEscapePuzzle(pieces, 1, 4, activeCells = active).borderExitShare, 1e-9)
    }
}
