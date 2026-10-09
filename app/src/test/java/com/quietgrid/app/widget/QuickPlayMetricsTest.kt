package com.quietgrid.app.widget

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPlayMetricsTest {

    @Test
    fun `single button at 2x2`() {
        val m = quickPlayMetrics(110.dp, 110.dp, 1)
        assertEquals(55f, m.iconSize.value, 0.01f)
        assertEquals(19.8f, m.textSize.value, 0.01f)
    }

    @Test
    fun `three buttons side by side when wider than tall`() {
        val m = quickPlayMetrics(250.dp, 110.dp, 3)
        assertFalse(m.vertical)
        assertEquals(250f / 3 * 0.5f, m.iconSize.value, 0.01f)
        assertEquals(16f, m.textSize.value, 0.01f)
    }

    @Test
    fun `three buttons stacked when taller than wide`() {
        assertTrue(quickPlayMetrics(110.dp, 300.dp, 3).vertical)
    }

    @Test
    fun `bigger widget gives bigger icon and text`() {
        val small = quickPlayMetrics(250.dp, 110.dp, 3)
        val big = quickPlayMetrics(500.dp, 220.dp, 3)
        assertTrue(big.iconSize > small.iconSize)
        assertTrue(big.textSize.value > small.textSize.value)
    }

    @Test
    fun `text size is capped`() {
        assertEquals(48f, quickPlayMetrics(1000.dp, 1000.dp, 1).textSize.value, 0.01f)
    }
}
