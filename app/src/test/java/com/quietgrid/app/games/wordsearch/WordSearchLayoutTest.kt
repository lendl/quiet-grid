package com.quietgrid.app.games.wordsearch

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchLayoutTest {

    @Test
    fun `wide tablet landscape region is side by side`() {
        assertTrue(shouldShowWordListSideBySide(regionWidth = 900.dp, regionHeight = 500.dp))
    }

    @Test
    fun `phone landscape region under 600dp stays stacked`() {
        assertFalse(shouldShowWordListSideBySide(regionWidth = 560.dp, regionHeight = 300.dp))
    }

    @Test
    fun `tablet portrait region taller than wide stays stacked`() {
        assertFalse(shouldShowWordListSideBySide(regionWidth = 700.dp, regionHeight = 900.dp))
    }

    @Test
    fun `region exactly at 600dp width and wider than tall is side by side`() {
        assertTrue(shouldShowWordListSideBySide(regionWidth = 600.dp, regionHeight = 400.dp))
    }

    @Test
    fun `square region is not side by side`() {
        assertFalse(shouldShowWordListSideBySide(regionWidth = 700.dp, regionHeight = 700.dp))
    }
}
