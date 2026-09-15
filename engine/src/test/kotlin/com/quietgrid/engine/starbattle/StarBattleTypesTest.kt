package com.quietgrid.engine.starbattle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleTypesTest {
    @Test
    fun `rejects wrong-sized region grid`() {
        assertFalse(isValidStarBattleRegionGrid(3, 1, listOf(listOf(0, 0), listOf(1, 1))))
    }

    @Test
    fun `rejects disconnected region`() {
        val regions = listOf(
            listOf(0, 1, 0),
            listOf(1, 1, 0),
            listOf(1, 0, 0),
        )
        assertFalse(isValidStarBattleRegionGrid(3, 1, regions))
    }

    @Test
    fun `accepts fully connected coverage for k=1`() {
        val regions = listOf(
            listOf(0, 0, 1),
            listOf(0, 1, 1),
            listOf(2, 2, 2),
        )
        assertTrue(isValidStarBattleRegionGrid(3, 1, regions))
    }

    @Test
    fun `rejects a 2-cell region for k=2 since its cells always touch`() {
        val regions = listOf(
            listOf(0, 0, 1),
            listOf(2, 2, 1),
            listOf(2, 2, 1),
        )
        assertFalse(isValidStarBattleRegionGrid(3, 2, regions))
    }

    @Test
    fun `accepts a straight 3-in-a-row region for k=2 since its ends are non-touching`() {
        val regions = listOf(
            listOf(0, 0, 0),
            listOf(1, 1, 1),
            listOf(2, 2, 2),
        )
        assertTrue(isValidStarBattleRegionGrid(3, 2, regions))
    }

    @Test
    fun `hasNonTouchingPair is true only for cells at chebyshev distance 2 or more`() {
        assertFalse(hasNonTouchingPair(listOf(0 to 0, 1 to 1)))
        assertTrue(hasNonTouchingPair(listOf(0 to 0, 0 to 2)))
    }
}
