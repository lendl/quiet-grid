package com.quietgrid.app.ui.components

import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameIconsTest {

    @Test
    fun `every game has a hue and a glyph`() {
        GameId.entries.forEach { id ->
            assertTrue("$id hue", GAME_ICON_HUES.containsKey(id))
            assertTrue("$id glyph", GAME_ICON_GLYPHS[id].orEmpty().isNotEmpty())
        }
    }

    @Test
    fun `animal doku is a two by two board with a paw in the top left cell`() {
        val shapes = GAME_ICON_GLYPHS.getValue(GameId.ANIMALDOKU)
        val cells = shapes.filterIsInstance<GlyphRect>()
        assertEquals(4, cells.size)
        assertTrue(cells.all { it.width == 9.5f && it.height == 9.5f })
        val topLeft = cells.single { it.x == 1.5f && it.y == 1.5f }
        assertEquals(GlyphPaint.ACCENT, topLeft.paint)
        val paw = shapes.filterIsInstance<GlyphOval>()
        assertEquals(4, paw.size)
        assertTrue(paw.all { it.paint == GlyphPaint.CUTOUT && it.cx in 1.5f..11f && it.cy in 1.5f..11f })
    }

    @Test
    fun `word guess shows guessed letters in its rows`() {
        val letters = GAME_ICON_GLYPHS.getValue(GameId.WORDGUESS).filterIsInstance<GlyphText>()
        assertEquals("CATCAR", letters.joinToString("") { it.text })
        assertTrue(letters.all { it.size >= 5f })
    }

    @Test
    fun `pencil glyph colors stay grayscale`() {
        val tile = Color(0xFFE0E0E0)
        val ink = Color(0xFF1A1A1A)
        GlyphPaint.entries.forEach { paint ->
            val color = glyphPaintColor(paint, tile = tile, ink = ink, isPencilTheme = true)
            assertTrue("$paint $color", color.red == color.green && color.green == color.blue)
        }
    }

    @Test
    fun `cutout shapes take the tile color so the glyph reads as a hole`() {
        val tile = Color(0xFFC25E3C)
        assertEquals(tile, glyphPaintColor(GlyphPaint.CUTOUT, tile = tile, ink = Color.White, isPencilTheme = false))
    }
}
