package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDokuTypesTest {
    @Test
    fun `isValidAnimalDokuRegionGrid accepts four connected 2x2 quadrant regions on a 4x4 grid`() {
        val regions = listOf(
            listOf(0, 0, 1, 1),
            listOf(0, 0, 1, 1),
            listOf(2, 2, 3, 3),
            listOf(2, 2, 3, 3),
        )
        assertTrue(isValidAnimalDokuRegionGrid(4, regions))
    }

    @Test
    fun `isValidAnimalDokuRegionGrid rejects a region split into two disconnected pieces`() {
        val regions = listOf(
            listOf(0, 1, 0, 1),
            listOf(1, 1, 1, 1),
            listOf(2, 2, 3, 3),
            listOf(2, 2, 3, 3),
        )
        assertFalse(isValidAnimalDokuRegionGrid(4, regions))
    }

    @Test
    fun `isValidAnimalDokuRegionGrid rejects a grid missing one of the N expected region ids`() {
        val regions = listOf(
            listOf(0, 0, 1, 1),
            listOf(0, 0, 1, 1),
            listOf(2, 2, 2, 2),
            listOf(2, 2, 2, 2),
        )
        assertFalse(isValidAnimalDokuRegionGrid(4, regions))
    }

    @Test
    fun `isValidAnimalDokuRegionGrid rejects a non-square regions list`() {
        val regions = listOf(
            listOf(0, 0, 1),
            listOf(0, 0, 1),
        )
        assertFalse(isValidAnimalDokuRegionGrid(4, regions))
    }
}
