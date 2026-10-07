package com.quietgrid.app.ui.screens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesGridLayoutTest {

    private fun forScreen(screenWidth: Int) = gamesGridLayout(screenWidth.dp - 32.dp)

    @Test
    fun `small phones show two games per row with bigger icons`() {
        listOf(280, 320).forEach { width ->
            val layout = forScreen(width)
            assertEquals("$width", 2, layout.columns)
            assertTrue("$width", layout.maxIconSize > 76.dp)
        }
    }

    @Test
    fun `regular phones keep three games per row at 76dp`() {
        listOf(360, 393, 411).forEach { width ->
            val layout = forScreen(width)
            assertEquals("$width", 3, layout.columns)
            assertEquals("$width", 76.dp, layout.maxIconSize)
            assertFalse("$width", layout.largeLabels)
        }
    }

    @Test
    fun `tablets get bigger icons and more columns as width grows`() {
        val small = forScreen(600)
        val portrait = forScreen(800)
        val landscape = forScreen(1280)
        listOf(small, portrait, landscape).forEach { layout ->
            assertTrue(layout.maxIconSize > 76.dp)
            assertTrue(layout.largeLabels)
        }
        assertTrue(portrait.columns > small.columns)
        assertTrue(landscape.columns > portrait.columns)
        assertTrue(landscape.columns <= 8)
    }
}
