package com.quietgrid.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPlayMetricsTest {

    private val defaultContent = DpSize(328.dp, 152.dp)

    @Test
    fun `three games sit side by side when wider than tall`() {
        val grid = quickPlayGrid(defaultContent, 3)
        assertFalse(grid.vertical)
        assertEquals(104f, grid.cell.width.value, 0.01f)
        assertEquals(152f, grid.cell.height.value, 0.01f)
    }

    @Test
    fun `three games stack when taller than wide`() {
        val grid = quickPlayGrid(DpSize(152.dp, 328.dp), 3)
        assertTrue(grid.vertical)
        assertEquals(104f, grid.cell.height.value, 0.01f)
    }

    @Test
    fun `continue strip takes about a quarter of the height within bounds`() {
        assertEquals(152f * 0.26f, quickPlayStripHeight(152.dp).value, 0.01f)
        assertEquals(32f, quickPlayStripHeight(94.dp).value, 0.01f)
        assertEquals(44f, quickPlayStripHeight(400.dp).value, 0.01f)
    }

    @Test
    fun `continue strip parts scale with its height`() {
        val metrics = quickPlayStripMetrics(40.dp)
        assertEquals(26f, metrics.circleSize.value, 0.01f)
        assertEquals(22f, metrics.iconSize.value, 0.01f)
        assertEquals(14f, metrics.labelSize.value, 0.01f)
        assertEquals(12f, quickPlayStripMetrics(32.dp).labelSize.value, 0.01f)
    }

    @Test
    fun `default tile puts text under the icon and keeps the icon inside the cell`() {
        val metrics = quickPlayTileMetrics(DpSize(104.dp, 152.dp))
        assertFalse(metrics.horizontal)
        assertEquals(15.6f, metrics.titleSize.value, 0.01f)
        assertTrue(metrics.maxIconSize.value <= 104f * 0.66f + 0.01f)
    }

    @Test
    fun `wide short tile puts text beside the icon`() {
        assertTrue(quickPlayTileMetrics(DpSize(300.dp, 100.dp)).horizontal)
    }

    @Test
    fun `bigger widget gives bigger icons and text, up to a cap`() {
        val small = quickPlayTileMetrics(DpSize(104.dp, 152.dp))
        val big = quickPlayTileMetrics(DpSize(160.dp, 260.dp))
        assertTrue(big.maxIconSize > small.maxIconSize)
        assertTrue(big.titleSize.value > small.titleSize.value)
        assertEquals(30f, quickPlayTileMetrics(DpSize(1000.dp, 1000.dp)).titleSize.value, 0.01f)
    }

    @Test
    fun `mix button at default size lays out in a row`() {
        val metrics = quickPlayButtonMetrics(DpSize(300.dp, 147.dp), MIX_TITLE_FRACTION)
        assertTrue(metrics.horizontal)
        assertEquals(147f * MIX_TITLE_FRACTION, metrics.titleSize.value, 0.01f)
        assertEquals(147f * MIX_TITLE_FRACTION * 0.6f, metrics.labelSize.value, 0.01f)
    }

    @Test
    fun `narrow button lays out in a column and title is capped`() {
        assertFalse(quickPlayButtonMetrics(DpSize(150.dp, 300.dp), MIX_TITLE_FRACTION).horizontal)
        assertEquals(40f, quickPlayButtonMetrics(DpSize(2000.dp, 1000.dp), MIX_TITLE_FRACTION).titleSize.value, 0.01f)
    }
}
