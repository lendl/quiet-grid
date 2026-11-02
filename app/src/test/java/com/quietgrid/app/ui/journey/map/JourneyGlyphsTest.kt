package com.quietgrid.app.ui.journey.map

import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.flatGlyphColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyGlyphsTest {
    private val layout = journeyRoadLayout(stopCount = 30, width = 390f)

    @Test
    fun `sixteen glyphs per page within size range`() {
        val glyphs = journeyGlyphs(layout, 11)
        val fullPages = (layout.height / JOURNEY_GLYPH_PAGE_HEIGHT).toInt()
        (0 until fullPages).forEach { page ->
            val top = page * JOURNEY_GLYPH_PAGE_HEIGHT
            assertEquals("page $page", JOURNEY_GLYPHS_PER_PAGE, glyphs.count { it.y >= top && it.y < top + JOURNEY_GLYPH_PAGE_HEIGHT })
        }
        assertTrue(glyphs.all { it.size in 16f..22f })
        assertTrue(glyphs.all { it.x - it.size / 2 >= 0f && it.x + it.size / 2 <= layout.width && it.y - it.size / 2 >= 0f && it.y + it.size / 2 <= layout.height })
    }

    @Test
    fun `no sudoku and no repeats in a row`() {
        listOf(1, 2, 3, 99).forEach { seed ->
            val glyphs = journeyGlyphs(layout, seed)
            assertFalse(glyphs.any { it.gameId == GameId.SUDOKU })
            glyphs.zipWithNext().forEach { (a, b) -> assertTrue("seed $seed", a.gameId != b.gameId) }
            assertTrue(glyphs.map { it.gameId }.toSet().size > 4)
        }
    }

    @Test
    fun `glyphs avoid the road`() {
        listOf(340f, 390f, 480f).forEach { width ->
            val wide = journeyRoadLayout(stopCount = 30, width = width)
            journeyGlyphs(wide, 5).forEach { glyph ->
                val half = glyph.size / 2
                val bounds = SceneBounds(glyph.x - half, glyph.y - half, glyph.x + half, glyph.y + half)
                assertTrue("width $width $glyph", wide.distanceToRoad(bounds) >= JOURNEY_SCENE_ROAD_CLEARANCE)
            }
        }
    }

    @Test
    fun `deterministic for a seed`() {
        assertEquals(journeyGlyphs(layout, 3), journeyGlyphs(layout, 3))
        assertTrue(journeyGlyphs(layout, 3) != journeyGlyphs(layout, 4))
    }

    @Test
    fun `flat glyph colour keeps one hue and skips cutouts`() {
        val ink = Color(0xFF223344).copy(alpha = 0.3f)
        assertEquals(null, flatGlyphColor(GlyphPaint.CUTOUT, ink))
        GlyphPaint.entries.filter { it != GlyphPaint.CUTOUT }.forEach { paint ->
            val colour = flatGlyphColor(paint, ink)!!
            assertEquals(paint.name, ink.copy(alpha = 1f), colour.copy(alpha = 1f))
            assertTrue(paint.name, colour.alpha <= ink.alpha)
        }
    }
}
