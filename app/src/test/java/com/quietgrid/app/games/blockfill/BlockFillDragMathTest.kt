package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockFillDragMathTest {
    private val cellSize = 40f
    private val originX = 100f
    private val originY = 200f

    @Test
    fun `resolveAnchorCell maps the piece top-left to the correct cell`() {
        val board = createEmptyBoard()
        val anchor = resolveAnchorCell(
            pieceLeftX = originX + 3 * cellSize + 5f,
            pieceTopY = originY + 2 * cellSize + 5f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0),
        )
        assertEquals(BlockFillDragAnchor(2, 3), anchor)
    }

    @Test
    fun `resolveAnchorCell snaps to the nearest cell once the piece is past halfway`() {
        val board = createEmptyBoard()
        val anchor = resolveAnchorCell(
            pieceLeftX = originX + 3 * cellSize + 25f,
            pieceTopY = originY + 2 * cellSize + 30f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0),
        )
        assertEquals(BlockFillDragAnchor(3, 4), anchor)
    }

    @Test
    fun `resolveAnchorCell snaps a piece slightly outside the board edge back onto it`() {
        val board = createEmptyBoard()
        val anchor = resolveAnchorCell(
            pieceLeftX = originX - 10f,
            pieceTopY = originY - 10f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0),
        )
        assertEquals(BlockFillDragAnchor(0, 0), anchor)
    }

    @Test
    fun `resolveAnchorCell returns null when the piece would hang off the board edge`() {
        val board = createEmptyBoard()
        val anchor = resolveAnchorCell(
            pieceLeftX = originX + (BLOCKFILL_BOARD_SIZE - 1) * cellSize + 5f,
            pieceTopY = originY + 5f,
            boardOriginX = originX,
            boardOriginY = originY,
            cellSizePx = cellSize,
            board = board,
            pieceCells = listOf(0 to 0, 0 to 1, 0 to 2),
        )
        assertNull(anchor)
    }

    @Test
    fun `resolveAnchorCell returns null over an occupied cell`() {
        val board = placePieceAt(createEmptyBoard(), listOf(0 to 0), 1, 1, BlockFillShapeFamily.SINGLE)
        val anchor = resolveAnchorCell(
            pieceLeftX = originX + 1 * cellSize + 5f,
            pieceTopY = originY + 1 * cellSize + 5f,
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

    @Test
    fun `floatingPieceTopLeft centers the piece horizontally and lifts it above the pointer`() {
        val rect3x2 = listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0, 2 to 1)
        val topLeft = floatingPieceTopLeft(
            pointerX = 500f,
            pointerY = 800f,
            pieceCells = rect3x2,
            cellSizePx = cellSize,
            liftPx = 72f,
        )
        assertEquals(BlockFillPiecePosition(x = 460f, y = 608f), topLeft)
    }

    @Test
    fun `the anchor resolved from the floating piece matches the cell under the piece, not the pointer`() {
        val board = createEmptyBoard()
        val single = listOf(0 to 0)
        val pointerX = originX + 4 * cellSize + 20f
        val pointerY = originY + 7 * cellSize + 5f
        val topLeft = floatingPieceTopLeft(pointerX, pointerY, single, cellSize, liftPx = 2 * cellSize)
        val anchor = resolveAnchorCell(topLeft.x, topLeft.y, originX, originY, cellSize, board, single)
        assertEquals(BlockFillDragAnchor(4, 4), anchor)
    }
}
