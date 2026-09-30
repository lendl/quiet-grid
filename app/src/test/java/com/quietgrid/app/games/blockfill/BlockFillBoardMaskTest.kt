package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillBoardMaskTest {
    private fun piece(id: String) = shapeDefToPiece(ALL_SHAPES.first { it.id == id })

    private fun boardWith(cells: Set<Pair<Int, Int>>): BlockFillBoard {
        var board = createEmptyBoard()
        for ((row, col) in cells) board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.STONE)
        return board
    }

    private val stack = boardWith((0..2).flatMap { r -> (0..4).map { r to it } }.toSet())

    @Test
    fun `canEmptyBoard finds the order that clears everything`() {
        val tray = listOf(piece("straight3-v"), piece("straight3-v"), piece("straight3-v"))
        assertTrue(canEmptyBoard(stack, tray))
        assertEquals(0, fewestBlocksLeft(stack, tray))
    }

    @Test
    fun `fewestBlocksLeft picks the best partial clear`() {
        val tray = listOf(piece("single"), piece("single"), piece("single"))
        assertEquals(10, fewestBlocksLeft(stack, tray))
        assertFalse(canEmptyBoard(stack, tray))
    }

    @Test
    fun `fewestBlocksLeft is null when the set cannot be fully placed`() {
        val holes = (0 until BLOCKFILL_BOARD_SIZE).map { row -> row to (row * 3) % BLOCKFILL_BOARD_SIZE }.toSet()
        val all = (0 until BLOCKFILL_BOARD_SIZE).flatMap { r -> (0 until BLOCKFILL_BOARD_SIZE).map { r to it } }.toSet()
        val tray = listOf(piece("domino-h"), piece("domino-h"), piece("domino-h"))
        assertNull(fewestBlocksLeft(boardWith(all - holes), tray))
    }

    @Test
    fun `toMask sets one bit per filled cell`() {
        assertEquals(1L or (1L shl 63), boardWith(setOf(0 to 0, 7 to 7)).toMask())
    }

    @Test
    fun `canEmptyBoard stays fast on a mid-density board`() {
        val start = System.nanoTime()
        for (seed in 1..20) {
            val board = generateClutter(BlockFillClutterStyle.MIRROR, 16, Random(seed))
            val tray = drawTray("expert", board, random = Random(seed)).filterNotNull()
            canEmptyBoard(board, tray)
            fewestBlocksLeft(board, tray)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $elapsedMs ms", elapsedMs < 2000)
    }
}
