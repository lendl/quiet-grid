package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillShapesTest {

    @Test
    fun `drawWeightedPiece on easy never returns a zero-weight family`() {
        val random = Random(42)
        repeat(500) {
            val piece = drawWeightedPiece("easy", random)
            assertTrue(piece.family != BlockFillShapeFamily.SZ)
            assertTrue(piece.family != BlockFillShapeFamily.DIAGONAL_STAIRCASE3)
            assertTrue(piece.family != BlockFillShapeFamily.PLUS)
        }
    }

    @Test
    fun `every shape id in ALL_SHAPES is unique`() {
        val ids = ALL_SHAPES.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every difficulty key has a weight entry for every shape family`() {
        for ((_, weights) in SHAPE_WEIGHTS_BY_DIFFICULTY) {
            for (family in BlockFillShapeFamily.entries) {
                assertTrue(weights.containsKey(family))
            }
        }
    }
}
