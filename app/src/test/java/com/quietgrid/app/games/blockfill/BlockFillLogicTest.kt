package com.quietgrid.app.games.blockfill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val FAMILY = BlockFillShapeFamily.SINGLE

class BlockFillLogicTest {

    @Test
    fun `canPlacePieceAt rejects out-of-bounds placement`() {
        val board = createEmptyBoard()
        assertFalse(canPlacePieceAt(board, listOf(0 to 0, 0 to 1, 0 to 2), 0, 7))
    }

    @Test
    fun `canPlacePieceAt rejects occupied cell`() {
        val board = placePieceAt(createEmptyBoard(), listOf(0 to 0), 3, 3, FAMILY)
        assertFalse(canPlacePieceAt(board, listOf(0 to 0), 3, 3))
    }

    @Test
    fun `canPlacePieceAt accepts open in-bounds placement`() {
        assertTrue(canPlacePieceAt(createEmptyBoard(), listOf(0 to 0, 0 to 1), 0, 0))
    }

    @Test
    fun `clearFullLines clears a single full row`() {
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE) board = placePieceAt(board, listOf(0 to 0), 2, col, FAMILY)
        val (cleared, linesCleared) = clearFullLines(board)
        assertEquals(1, linesCleared)
        assertTrue(cleared[2].all { it == null })
    }

    @Test
    fun `clearFullLines clears a single full column`() {
        var board = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) board = placePieceAt(board, listOf(0 to 0), row, 5, FAMILY)
        val (cleared, linesCleared) = clearFullLines(board)
        assertEquals(1, linesCleared)
        assertTrue(cleared.all { it[5] == null })
    }

    @Test
    fun `clearFullLines clears row and column simultaneously`() {
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE) board = placePieceAt(board, listOf(0 to 0), 0, col, FAMILY)
        for (row in 0 until BLOCKFILL_BOARD_SIZE) board = placePieceAt(board, listOf(0 to 0), row, 0, FAMILY)
        val (_, linesCleared) = clearFullLines(board)
        assertEquals(2, linesCleared)
    }

    @Test
    fun `clearFullLines is a no-op when nothing is full`() {
        val board = placePieceAt(createEmptyBoard(), listOf(0 to 0), 0, 0, FAMILY)
        val (cleared, linesCleared) = clearFullLines(board)
        assertEquals(0, linesCleared)
        assertEquals(board, cleared)
    }

    @Test
    fun `previewClearedCells returns empty for invalid placement`() {
        val board = placePieceAt(createEmptyBoard(), listOf(0 to 0), 0, 0, FAMILY)
        assertTrue(previewClearedCells(board, listOf(0 to 0), 0, 0, FAMILY).isEmpty())
    }

    @Test
    fun `previewClearedCells returns the cells a placement would clear`() {
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 4, col, FAMILY)
        val preview = previewClearedCells(board, listOf(0 to 0), 4, BLOCKFILL_BOARD_SIZE - 1, FAMILY)
        assertEquals(BLOCKFILL_BOARD_SIZE, preview.size)
    }

    @Test
    fun `isBoardEmpty is true only for an empty board`() {
        assertTrue(isBoardEmpty(createEmptyBoard()))
        assertFalse(isBoardEmpty(placePieceAt(createEmptyBoard(), listOf(0 to 0), 0, 0, FAMILY)))
    }
}
