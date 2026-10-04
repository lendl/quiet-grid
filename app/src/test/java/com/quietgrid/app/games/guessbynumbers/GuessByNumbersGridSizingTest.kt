package com.quietgrid.app.games.guessbynumbers

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuessByNumbersGridSizingTest {
    private val labelHeight = 16.dp

    @Test
    fun `all rows fit in the height left above the keyboard`() {
        val maxHeight = 520.dp
        val tileSize = guessByNumbersTileSize(
            maxWidth = 379.dp,
            maxHeight = maxHeight,
            wordLength = 5,
            maxGuesses = GUESS_BY_NUMBERS_MAX_GUESSES,
            labelHeight = labelHeight,
        )
        val gridHeight = (tileSize + labelHeight) * GUESS_BY_NUMBERS_MAX_GUESSES +
            6.dp * (GUESS_BY_NUMBERS_MAX_GUESSES - 1)
        assertTrue("grid $gridHeight exceeds $maxHeight", gridHeight <= maxHeight)
    }

    @Test
    fun `tile size stays at its maximum when there is plenty of room`() {
        val tileSize = guessByNumbersTileSize(
            maxWidth = 800.dp,
            maxHeight = 1200.dp,
            wordLength = 5,
            maxGuesses = GUESS_BY_NUMBERS_MAX_GUESSES,
            labelHeight = labelHeight,
        )
        assertEquals(40.dp, tileSize)
    }

    @Test
    fun `tile size is never negative in a tiny split-screen window`() {
        val tileSize = guessByNumbersTileSize(
            maxWidth = 300.dp,
            maxHeight = 120.dp,
            wordLength = 5,
            maxGuesses = GUESS_BY_NUMBERS_MAX_GUESSES,
            labelHeight = labelHeight,
        )
        assertTrue(tileSize >= 0.dp)
    }
}
