package com.quietgrid.app.games.blockfill

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `every difficulty key has a weight entry for every tray shape family`() {
        for ((_, weights) in SHAPE_WEIGHTS_BY_DIFFICULTY) {
            for (family in BlockFillShapeFamily.entries - BlockFillShapeFamily.STONE) {
                assertTrue(weights.containsKey(family))
            }
        }
    }

    private fun cellsOf(id: String) = ALL_SHAPES.first { it.id == id }.cells

    @Test
    fun `diagonal pieces get one bridge per corner-touching pair`() {
        assertEquals(1, diagonalBridges(cellsOf("diagonal-domino-a")).size)
        assertEquals(1, diagonalBridges(cellsOf("diagonal-domino-b")).size)
        assertEquals(2, diagonalBridges(cellsOf("diagonal-staircase3-a")).size)
        assertEquals(2, diagonalBridges(cellsOf("diagonal-staircase3-b")).size)
    }

    @Test
    fun `connected pieces get no bridges`() {
        for (shape in ALL_SHAPES.filter { it.family != BlockFillShapeFamily.DIAGONAL_DOMINO && it.family != BlockFillShapeFamily.DIAGONAL_STAIRCASE3 }) {
            assertEquals(shape.id, 0, diagonalBridges(shape.cells).size)
        }
    }

    @Test
    fun `singles are rare on every difficulty`() {
        val expectedSingles = mapOf("easy" to 8, "medium" to 5, "hard" to 3, "expert" to 2)
        for ((difficulty, weights) in SHAPE_WEIGHTS_BY_DIFFICULTY) {
            assertEquals(difficulty, expectedSingles.getValue(difficulty), weights.getValue(BlockFillShapeFamily.SINGLE))
        }
    }

    @Test
    fun `stone is never dealt in the tray`() {
        for ((_, weights) in SHAPE_WEIGHTS_BY_DIFFICULTY) assertFalse(weights.containsKey(BlockFillShapeFamily.STONE))
        assertTrue(ALL_SHAPES.none { it.family == BlockFillShapeFamily.STONE })
    }
}
