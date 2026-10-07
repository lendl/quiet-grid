package com.quietgrid.app.games.minesweeper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesweeperPaletteTest {

    private fun contrast(a: Color, b: Color): Float {
        val high = maxOf(a.luminance(), b.luminance())
        val low = minOf(a.luminance(), b.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test
    fun `light surfaces use the light palette and dark surfaces the dark palette`() {
        assertSame(MINESWEEPER_LIGHT_PALETTE, minesweeperPaletteFor(Color(0xFFFFFFFF)))
        assertSame(MINESWEEPER_LIGHT_PALETTE, minesweeperPaletteFor(Color(0xFFF5F7FA)))
        assertSame(MINESWEEPER_DARK_PALETTE, minesweeperPaletteFor(Color(0xFF161B22)))
        assertSame(MINESWEEPER_DARK_PALETTE, minesweeperPaletteFor(Color(0xFF232530)))
    }

    @Test
    fun `every number reads clearly on the opened tile`() {
        listOf(MINESWEEPER_LIGHT_PALETTE, MINESWEEPER_DARK_PALETTE).forEach { palette ->
            assertEquals(8, palette.numbers.size)
            palette.numbers.forEachIndexed { index, color ->
                val ratio = contrast(color, palette.opened)
                assertTrue("number ${index + 1} contrast $ratio", ratio >= 4.0f)
            }
        }
    }
}
