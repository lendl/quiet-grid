package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillEndlessDealerTest {
    private val stack: BlockFillBoard = run {
        var board = createEmptyBoard()
        for (row in 0..2) for (col in 0..4) board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.STONE)
        board
    }

    private fun leftOrMax(board: BlockFillBoard, set: List<BlockFillPiece>) = fewestBlocksLeft(board, set) ?: Int.MAX_VALUE

    @Test
    fun `no steering inside the calm window`() {
        for (seed in 1..10) {
            assertEquals(
                "seed $seed",
                drawTray(endlessLevelFor(1).rules, stack, random = Random(seed)),
                drawEndlessTray(stack, 1, 10, Random(seed)),
            )
        }
    }

    @Test
    fun `steering builds a set that completes a nearly full line`() {
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 0, col, BlockFillShapeFamily.STONE)
        val dealt = drawEndlessTray(board, 1, Random(1), steer = true).filterNotNull()
        val completesLine = dealt.any { piece ->
            findValidPlacements(board, piece.cells).any { (row, col) ->
                clearFullLines(placePieceAt(board, piece.cells, row, col, piece.family)).second > 0
            }
        }
        assertTrue(completesLine)
        assertEquals(3, dealt.size)
    }

    @Test
    fun `steering only hands out a single when it finishes a line`() {
        for (seed in 1..20) {
            val dealt = drawEndlessTray(stack, 1, Random(seed), steer = true).filterNotNull()
            assertTrue("seed $seed dealt ${dealt.map { it.shapeId }}", dealt.none { it.family == BlockFillShapeFamily.SINGLE })
        }
        var gap = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) gap = placePieceAt(gap, listOf(0 to 0), 0, col, BlockFillShapeFamily.STONE)
        val dealt = drawEndlessTray(gap, 1, Random(1), steer = true).filterNotNull()
        assertTrue("first helper should finish the row: ${dealt.map { it.shapeId }}", dealt.any { it.family == BlockFillShapeFamily.SINGLE })
    }

    @Test
    fun `steered sets are not always the same`() {
        val sets = (1..20).map { seed -> drawEndlessTray(stack, 1, Random(seed), steer = true).filterNotNull().map { it.shapeId }.sorted() }.toSet()
        assertTrue("only ${sets.size} distinct steered sets", sets.size > 1)
    }

    @Test
    fun `steered sets respect the level guarantee`() {
        for (seed in 1..10) {
            val board = generateClutter(BlockFillClutterStyle.MIRROR, 16, Random(seed))
            val dealt = drawEndlessTray(board, 9, Random(seed), steer = true).filterNotNull()
            assertTrue("seed $seed", canPlaceAtLeast(board, dealt, 1))
        }
    }
}
