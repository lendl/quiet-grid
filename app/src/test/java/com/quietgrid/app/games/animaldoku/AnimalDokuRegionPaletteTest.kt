package com.quietgrid.app.games.animaldoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDokuRegionPaletteTest {

    @Test
    fun `palettes keep nine original colors`() {
        assertEquals(9, ANIMALDOKU_REGION_PALETTE_LIGHT.size)
        assertEquals(9, ANIMALDOKU_REGION_PALETTE_DARK.size)
    }

    @Test
    fun `forbidden pairs are ordered and point inside their palette`() {
        listOf(
            ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT to ANIMALDOKU_REGION_PALETTE_LIGHT.size,
            ANIMALDOKU_FORBIDDEN_PAIRS_DARK to ANIMALDOKU_REGION_PALETTE_DARK.size,
        ).forEach { (pairs, size) ->
            assertTrue(pairs.isNotEmpty())
            pairs.forEach { (a, b) -> assertTrue("$a-$b", a < b && b < size) }
        }
    }

    @Test
    fun `pairs named in the issue never touch`() {
        assertTrue(5 to 6 in ANIMALDOKU_FORBIDDEN_PAIRS_DARK)
        assertTrue(2 to 4 in ANIMALDOKU_FORBIDDEN_PAIRS_DARK)
        assertTrue(6 to 8 in ANIMALDOKU_FORBIDDEN_PAIRS_DARK)
        assertTrue(3 to 8 in ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT)
        assertTrue(3 to 5 in ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT)
        assertTrue(1 to 2 in ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT)
    }
}
