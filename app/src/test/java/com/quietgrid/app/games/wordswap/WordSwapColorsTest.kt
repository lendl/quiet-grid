package com.quietgrid.app.games.wordswap

import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class WordSwapColorsTest {

    private fun distance(a: Color, b: Color): Float {
        val r = a.red - b.red
        val g = a.green - b.green
        val bl = a.blue - b.blue
        return sqrt(r * r + g * g + bl * bl)
    }

    private fun assertApart(theme: ResolvedTheme, label: String, a: Color, b: Color, minimum: Float) {
        val d = distance(a, b)
        assertTrue("$theme $label distance $d < $minimum", d >= minimum)
    }

    @Test
    fun tileStatesStayApartInEveryTheme() {
        ResolvedTheme.entries.forEach { theme ->
            val colors = wordSwapColorsFor(colorSchemeFor(theme), theme == ResolvedTheme.PENCIL)
            assertApart(theme, "selected/tile", colors.selected, colors.tile, 0.25f)
            assertApart(theme, "selected/done", colors.selected, colors.done, 0.25f)
            assertApart(theme, "hint/tile", colors.hint, colors.tile, 0.25f)
            assertApart(theme, "hint/done", colors.hint, colors.done, 0.25f)
            assertApart(theme, "selected/hint", colors.selected, colors.hint, 0.15f)
            assertApart(theme, "done/tile", colors.done, colors.tile, 0.07f)
        }
    }
}
