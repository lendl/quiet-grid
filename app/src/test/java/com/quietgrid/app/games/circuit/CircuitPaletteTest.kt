package com.quietgrid.app.games.circuit

import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CircuitPaletteTest {

    private fun distance(a: Color, b: Color): Float {
        val r = a.red - b.red
        val g = a.green - b.green
        val bl = a.blue - b.blue
        return sqrt(r * r + g * g + bl * bl)
    }

    @Test
    fun tileTonesStayApartInEveryTheme() {
        val failures = mutableListOf<String>()
        fun check(theme: ResolvedTheme, label: String, a: Color, b: Color, minimum: Float) {
            val d = distance(a, b)
            if (d < minimum) failures += "$theme $label distance $d < $minimum"
        }
        ResolvedTheme.entries.forEach { theme ->
            val palette = circuitPaletteFor(colorSchemeFor(theme), theme == ResolvedTheme.PENCIL)
            check(theme, "hint/tile", palette.hintTile, palette.tile, 0.25f)
            check(theme, "hint/locked", palette.hintTile, palette.lockedTile, 0.15f)
            check(theme, "locked/tile", palette.lockedTile, palette.tile, 0.07f)
            check(theme, "hint/litWire", palette.hintTile, palette.litWire, 0.3f)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
