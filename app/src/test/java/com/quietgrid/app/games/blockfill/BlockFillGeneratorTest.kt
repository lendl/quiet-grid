package com.quietgrid.app.games.blockfill

import com.quietgrid.app.core.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillGeneratorTest {

    @Test
    fun `bestClearFractionForTray is null when no piece fits anywhere`() {
        var fullBoard = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) fullBoard = placePieceAt(fullBoard, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
        }
        // Only the last column of each row is open (1-wide) — a straight5 piece cannot fit anywhere.
        val straight5 = ALL_SHAPES.first { it.id == "straight5-h" }
        val tray = listOf(shapeDefToPiece(straight5), shapeDefToPiece(straight5), shapeDefToPiece(straight5))
        assertNull(bestClearFractionForTray(fullBoard, tray))
    }

    @Test
    fun `bestClearFractionForTray finds a fit on an empty board`() {
        val single = shapeDefToPiece(ALL_SHAPES.first { it.id == "single" })
        val fraction = bestClearFractionForTray(createEmptyBoard(), listOf(single, single, single))
        assertNotNull(fraction)
    }

    @Test
    fun `drawTray always returns 3 pieces`() {
        val tray = drawTray("medium", createEmptyBoard(), refillRetryCap = 25, random = Random(1))
        assertEquals(3, tray.size)
        assertTrue(tray.all { it != null })
    }

    @Test
    fun `drawTray falls back to a guaranteed single piece when retry budget is exhausted`() {
        var fullBoard = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) fullBoard = placePieceAt(fullBoard, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
        }
        val tray = drawTray("expert", fullBoard, refillRetryCap = 1, random = Random(7))
        assertEquals("single", tray[0]?.shapeId)
    }

    @Test
    fun `createBlockFillSession produces a session with the right score target`() {
        val session = createBlockFillSession("easy", random = Random(3))
        assertEquals(300, session.puzzle.scoreTarget)
        assertEquals(3, session.tray.size)
        assertEquals(BlockFillStatus.PLAYING, session.status)
    }

    @Test
    fun `createBlockFillSession produces the right score target for every difficulty`() {
        // Guards against a Difficulty#key <-> BLOCKFILL_DIFFICULTY_CONFIG / SHAPE_WEIGHTS_BY_DIFFICULTY
        // map-key mismatch for hard/medium/expert, which would otherwise pass every other existing
        // test (they all exercise "easy" only).
        val expectedScoreTargets = mapOf(
            Difficulty.EASY to 300,
            Difficulty.MEDIUM to 600,
            Difficulty.HARD to 1000,
            Difficulty.EXPERT to 1500,
        )
        for (difficulty in Difficulty.entries) {
            val session = createBlockFillSession(difficulty.key, random = Random(1))
            assertEquals(
                "unexpected scoreTarget for difficulty ${difficulty.key}",
                expectedScoreTargets.getValue(difficulty),
                session.puzzle.scoreTarget,
            )
        }
    }
}
