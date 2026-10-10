package com.quietgrid.app.games.animaldoku

import com.quietgrid.engine.animaldoku.ANIMALDOKU_SIZES_BY_DIFFICULTY
import com.quietgrid.engine.starbattle.STARBATTLE_K2_SIZES_BY_GRADE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDokuRegionPaletteTest {

    @Test
    fun `palettes keep nine original colors plus one for ten-region grids`() {
        assertEquals(10, ANIMALDOKU_REGION_PALETTE_LIGHT.size)
        assertEquals(10, ANIMALDOKU_REGION_PALETTE_DARK.size)
    }

    @Test
    fun `every region of the largest generated grid gets its own color and animal`() {
        val largestGrid = (ANIMALDOKU_SIZES_BY_DIFFICULTY.values.flatten() + STARBATTLE_K2_SIZES_BY_GRADE.values.flatten()).max()
        assertTrue(ANIMALDOKU_REGION_PALETTE_LIGHT.size >= largestGrid)
        assertTrue(ANIMALDOKU_REGION_PALETTE_DARK.size >= largestGrid)
        assertTrue(ANIMAL_EMOJI_PALETTE.size >= largestGrid)
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
