package com.quietgrid.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class CelebrationGoldTest {
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    private fun goldFor(theme: ResolvedTheme): CelebrationGold {
        val scheme = colorSchemeFor(theme)
        return celebrationGoldFor(theme == ResolvedTheme.PENCIL, scheme.background, scheme.onBackground)
    }

    @Test
    fun `gold button text passes 4,5 to 1 on every theme`() {
        ResolvedTheme.entries.forEach { theme ->
            val gold = goldFor(theme)
            val ratio = contrast(gold.gold, gold.onGold)
            assertTrue("$theme contrast $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun `pencil gold stays grayscale`() {
        val gold = goldFor(ResolvedTheme.PENCIL)
        listOf(gold.gold, gold.onGold).forEach { color ->
            assertEquals(color.red, color.green, 0.001f)
            assertEquals(color.green, color.blue, 0.001f)
        }
    }

    @Test
    fun `dark backgrounds get the bright gold and light backgrounds the deep gold`() {
        assertEquals(Color(0xFFE8C46A), goldFor(ResolvedTheme.DARK).gold)
        assertEquals(Color(0xFFE8C46A), goldFor(ResolvedTheme.COFFEE).gold)
        assertEquals(Color(0xFFE8C46A), goldFor(ResolvedTheme.DRACULA).gold)
        assertEquals(Color(0xFFB7860B), goldFor(ResolvedTheme.LIGHT).gold)
        assertEquals(Color(0xFFB7860B), goldFor(ResolvedTheme.SILK).gold)
        assertEquals(Color(0xFFB7860B), goldFor(ResolvedTheme.NORD).gold)
    }
}
