package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillEndlessSessionTest {
    private fun piece(id: String) = shapeDefToPiece(ALL_SHAPES.first { it.id == id })

    private fun session(board: BlockFillBoard, tray: List<BlockFillPiece?>, score: Int = 0, movesSinceClear: Int = 0) =
        BlockFillEndlessSession(
            board = board,
            tray = tray,
            score = score,
            comboStreak = 0,
            multiplier = 1,
            moves = 0,
            movesSinceClear = movesSinceClear,
            linesCleared = 0,
            bestAtStart = 0,
            status = BlockFillStatus.PLAYING,
        )

    private fun topRowMissingLastCell(): BlockFillBoard {
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 0, col, BlockFillShapeFamily.STONE)
        return board
    }

    @Test
    fun `a fresh run starts on an easy pattern`() {
        for (seed in 1..20) {
            val run = createBlockFillEndlessSession(bestAtStart = 0, random = Random(seed))
            val blocks = run.board.flatten().filterNotNull()
            assertTrue("seed $seed has ${blocks.size} blocks", blocks.size in 8..12)
            assertTrue(blocks.all { it == BlockFillShapeFamily.STONE })
            assertEquals(1, run.multiplier)
            assertTrue(canPlaceTrayInSomeOrder(run.board, run.tray.filterNotNull()))
        }
    }

    @Test
    fun `a full clear raises the level and leaves the board empty`() {
        val start = session(topRowMissingLastCell(), listOf(piece("single"), piece("single"), piece("single")))
        val next = applyBlockFillEndlessPlacement(start, 0, 0, BLOCKFILL_BOARD_SIZE - 1, Random(1))
        assertNotNull(next)
        assertEquals(2, next!!.multiplier)
        assertEquals(0, countFilledCells(next.board))
        assertEquals(0, next.movesSinceClear)
        assertEquals(80 + 300, next.score)
        assertEquals(1, next.linesCleared)
    }

    @Test
    fun `moves since clear counts up and lines accumulate`() {
        val start = session(createEmptyBoard(), listOf(piece("single"), piece("single"), piece("single")), movesSinceClear = 5)
        val next = applyBlockFillEndlessPlacement(start, 0, 5, 5, Random(1))!!
        assertEquals(6, next.movesSinceClear)
        assertEquals(1, next.moves)
        assertEquals(0, next.linesCleared)
    }

    @Test
    fun `endless never wins`() {
        val start = session(createEmptyBoard(), listOf(piece("single"), piece("single"), piece("single")), score = 1_000_000)
        assertEquals(BlockFillStatus.PLAYING, applyBlockFillEndlessPlacement(start, 0, 5, 5, Random(1))!!.status)
    }

    @Test
    fun `endless ends when nothing fits`() {
        val emptyCells = setOf(0 to 0, 0 to 2, 2 to 0, 1 to 1, 3 to 3, 4 to 4, 5 to 5, 6 to 6, 7 to 7)
        var board = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                if (row to col !in emptyCells) board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
            }
        }
        val start = session(board, listOf(piece("single"), piece("domino-h"), piece("domino-h")))
        assertEquals(BlockFillStatus.LOST, applyBlockFillEndlessPlacement(start, 0, 0, 0, Random(1))!!.status)
    }
}
