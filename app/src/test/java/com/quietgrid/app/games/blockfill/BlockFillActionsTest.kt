package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillActionsTest {

    private fun freshSession(scoreTarget: Int = 300): BlockFillSession {
        val single = shapeDefToPiece(ALL_SHAPES.first { it.id == "single" })
        return BlockFillSession(
            puzzle = BlockFillPuzzle(id = "test", difficulty = "easy", scoreTarget = scoreTarget),
            board = createEmptyBoard(),
            tray = listOf(single, single, single),
            score = 0,
            comboStreak = 0,
            status = BlockFillStatus.PLAYING,
        )
    }

    @Test
    fun `applyBlockFillPlacement returns null for an illegal placement`() {
        val session = freshSession()
        val occupied = session.copy(board = placePieceAt(session.board, listOf(0 to 0), 0, 0, BlockFillShapeFamily.SINGLE))
        assertNull(applyBlockFillPlacement(occupied, pieceIndex = 0, anchorRow = 0, anchorCol = 0))
    }

    @Test
    fun `applyBlockFillPlacement returns null when session is not playing`() {
        val session = freshSession().copy(status = BlockFillStatus.WON)
        assertNull(applyBlockFillPlacement(session, pieceIndex = 0, anchorRow = 0, anchorCol = 0))
    }

    @Test
    fun `applyBlockFillPlacement scores nothing for a bare placement`() {
        val session = freshSession()
        val result = applyBlockFillPlacement(session, pieceIndex = 0, anchorRow = 0, anchorCol = 0, random = Random(1))
        assertNotNull(result)
        assertEquals(0, result!!.score)
        assertEquals(BlockFillStatus.PLAYING, result.status)
    }

    @Test
    fun `applyBlockFillPlacement refills the tray once all 3 slots are placed`() {
        var session = freshSession()
        session = applyBlockFillPlacement(session, 0, 0, 0, random = Random(1))!!
        session = applyBlockFillPlacement(session, 1, 0, 1, random = Random(1))!!
        session = applyBlockFillPlacement(session, 2, 0, 2, random = Random(1))!!
        assertEquals(3, session.tray.count { it != null })
    }

    @Test
    fun `applyBlockFillPlacement wins when score target is reached`() {
        val session = freshSession(scoreTarget = 1)
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 0, col, BlockFillShapeFamily.SINGLE)
        val nearlyFull = session.copy(board = board)
        val result = applyBlockFillPlacement(nearlyFull, pieceIndex = 0, anchorRow = 0, anchorCol = BLOCKFILL_BOARD_SIZE - 1, random = Random(1))
        assertEquals(BlockFillStatus.WON, result!!.status)
    }

    @Test
    fun `applyBlockFillPlacement loses when no piece in the tray fits`() {
        // Leave exactly one empty cell per row and per column (a permutation of gaps), with two
        // extra gaps folded into row 0 / column 0 so placing the single piece at (0, 0) doesn't
        // complete either line. Every other row/column keeps exactly one gap, so clearFullLines
        // clears nothing (nothing was ever fully filled), no tray refill is triggered, and — since
        // no row or column ever has more than one empty cell — the leftover dominoes in the tray
        // have no pair of adjacent empty cells anywhere on the board to land on -> LOST.
        val single = shapeDefToPiece(ALL_SHAPES.first { it.id == "single" })
        val domino = shapeDefToPiece(ALL_SHAPES.first { it.id == "domino-h" })
        val emptyCells = setOf(
            0 to 0, 0 to 2, 2 to 0,
            1 to 1, 3 to 3, 4 to 4, 5 to 5, 6 to 6, 7 to 7,
        )
        var board = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                if (row to col in emptyCells) continue
                board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
            }
        }
        val session = BlockFillSession(
            puzzle = BlockFillPuzzle(id = "test", difficulty = "easy", scoreTarget = 999_999),
            board = board,
            tray = listOf(single, domino, domino),
            score = 0,
            comboStreak = 0,
            status = BlockFillStatus.PLAYING,
        )
        val result = applyBlockFillPlacement(session, pieceIndex = 0, anchorRow = 0, anchorCol = 0, random = Random(1))
        assertNotNull(result)
        assertEquals(BlockFillStatus.LOST, result!!.status)
    }

    @Test
    fun `applyBlockFillPlacement accumulates combo streak across consecutive line clears`() {
        // Row 0 and row 1 each have 7 of 8 cells filled; a spare filled cell at (5, 5) keeps the
        // board non-empty after either line clears, so neither placement gets the full-board-clear
        // bonus muddying the comparison. Placing the tray's two singles at (0, 7) then (1, 7) clears
        // one line each -- the second placement's comboStreakBeforeThisMove should be 1 (carried
        // over from the first clear), so it must score strictly more than the first.
        val single = shapeDefToPiece(ALL_SHAPES.first { it.id == "single" })
        var board = createEmptyBoard()
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 0, col, BlockFillShapeFamily.SINGLE)
        for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) board = placePieceAt(board, listOf(0 to 0), 1, col, BlockFillShapeFamily.SINGLE)
        board = placePieceAt(board, listOf(0 to 0), 5, 5, BlockFillShapeFamily.SINGLE)

        val session = BlockFillSession(
            puzzle = BlockFillPuzzle(id = "test", difficulty = "easy", scoreTarget = 999_999),
            board = board,
            tray = listOf(single, single, single),
            score = 0,
            comboStreak = 0,
            status = BlockFillStatus.PLAYING,
        )

        val afterFirst = applyBlockFillPlacement(session, pieceIndex = 0, anchorRow = 0, anchorCol = 7, random = Random(1))!!
        assertEquals(1, afterFirst.comboStreak)
        val firstGain = afterFirst.score - session.score

        val afterSecond = applyBlockFillPlacement(afterFirst, pieceIndex = 1, anchorRow = 1, anchorCol = 7, random = Random(1))!!
        assertEquals(2, afterSecond.comboStreak)
        val secondGain = afterSecond.score - afterFirst.score

        assertTrue("second clear's combo bonus ($secondGain) should exceed the first's ($firstGain)", secondGain > firstGain)
    }
}
