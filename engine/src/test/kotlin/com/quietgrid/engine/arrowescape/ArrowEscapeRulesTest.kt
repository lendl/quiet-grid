package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeRulesTest {
    private val pieces = listOf(
        ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.DOWN),
        ArrowEscapePiece(cells = listOf(CellCoord(1, 0)), headDirection = ArrowDirection.DOWN),
    )

    @Test
    fun `piece blocked by an unremoved piece is not removable`() {
        val ownerLookup = buildCellOwnerMap(pieces)
        assertFalse(isPieceRemovable(0, pieces, removedIndices = emptySet(), ownerLookup = ownerLookup, rows = 2, cols = 1))
    }

    @Test
    fun `piece is removable once the blocking piece is removed`() {
        val ownerLookup = buildCellOwnerMap(pieces)
        assertTrue(isPieceRemovable(0, pieces, removedIndices = setOf(1), ownerLookup = ownerLookup, rows = 2, cols = 1))
    }

    @Test
    fun `piece whose corridor exits straight off the board is removable`() {
        val ownerLookup = buildCellOwnerMap(pieces)
        assertTrue(isPieceRemovable(1, pieces, removedIndices = emptySet(), ownerLookup = ownerLookup, rows = 2, cols = 1))
    }

    @Test
    fun `an arrow pointing into its own body is never removable`() {
        val hook = listOf(
            ArrowEscapePiece(
                cells = listOf(CellCoord(3, 1), CellCoord(2, 1), CellCoord(2, 0), CellCoord(1, 0), CellCoord(0, 0), CellCoord(0, 1), CellCoord(1, 1)),
                headDirection = ArrowDirection.DOWN,
            ),
        )
        assertFalse(isPieceRemovable(0, hook, removedIndices = emptySet(), ownerLookup = buildCellOwnerMap(hook), rows = 4, cols = 2))
    }

    @Test
    fun `findNextRemovablePiece returns the first removable index and null when none exist`() {
        assertEquals(1, findNextRemovablePiece(pieces, removedIndices = emptySet(), rows = 2, cols = 1))
        assertNull(findNextRemovablePiece(pieces, removedIndices = setOf(0, 1), rows = 2, cols = 1))
    }
}
