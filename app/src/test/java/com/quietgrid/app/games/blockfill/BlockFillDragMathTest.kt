package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockFillDragMathTest {
    private val cellSize = 40f
    private val originX = 100f
    private val originY = 200f

    @Test
    fun `resolveAnchorCell maps a pointer position to the correct cell`() {
        val board = createEmptyBoard()
        // Pointer over cell (2, 3): originX + 3*cellSize + half, originY + 2*cellSize + half
        val anchor = resolveAnchorCell(
            pointerX = originX + 3 * cellSize + 5f,
            pointerY = originY + 2 * cellSize + 5f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0),
        )
        assertEquals(BlockFillDragAnchor(2, 3), anchor)
    }

    @Test
    fun `resolveAnchorCell returns null when the piece would hang off the board edge`() {
        val board = createEmptyBoard()
        val anchor = resolveAnchorCell(
            pointerX = originX + (BLOCKFILL_BOARD_SIZE - 1) * cellSize + 5f,
            pointerY = originY + 5f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0, 0 to 1, 0 to 2), // 3-wide piece, anchored at the last column
        )
        assertNull(anchor)
    }

    @Test
    fun `resolveAnchorCell returns null over an occupied cell`() {
        val board = placePieceAt(createEmptyBoard(), listOf(0 to 0), 1, 1, BlockFillShapeFamily.SINGLE)
        val anchor = resolveAnchorCell(
            pointerX = originX + 1 * cellSize + 5f,
            pointerY = originY + 1 * cellSize + 5f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0),
        )
        assertNull(anchor)
    }

    @Test
    fun `resolveAnchorCell returns null for zero cell size`() {
        assertNull(resolveAnchorCell(0f, 0f, 0f, 0f, 0f, createEmptyBoard(), listOf(0 to 0)))
    }
}
