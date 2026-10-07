package com.quietgrid.app.games.whodidit

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItLayoutTest {
    private val shapes = listOf(3 to 2, 4 to 2, 4 to 3, 5 to 3)
    private val densities = listOf(1f, 2f, 2.625f, 3f, 3.5f)

    @Test
    fun `grid metrics always fit the staircase inside the slot`() {
        for ((size, groups) in shapes) {
            for (density in densities) {
                for (width in 40..1400 step 37) {
                    for (height in 40..1400 step 41) {
                        val m = whoDidItGridMetrics(width, height, density, size, groups, groups)
                        val label = "size=$size groups=$groups density=$density ${width}x$height -> $m"
                        assertTrue(label, m.cell >= 0)
                        assertTrue(label, m.rowLabelWidth + groups * (m.gap + size * m.cell) <= width)
                        assertTrue(label, m.columnLabelHeight + groups * (m.gap + size * m.cell) <= height)
                        if (width >= 300 && height >= 160) assertTrue(label, m.cell > 0)
                    }
                }
            }
        }
    }

    @Test
    fun `grid metrics fit an expert board into a phone slot without a floor`() {
        val m = whoDidItGridMetrics(312, 300, 1f, 5, 3, 3)
        assertEquals(58, m.rowLabelWidth)
        assertEquals(62, m.columnLabelHeight)
        assertEquals(15, m.cell)
    }

    @Test
    fun `grid metrics cap the cell on roomy or unbounded slots`() {
        assertEquals(44, whoDidItGridMetrics(2000, 2000, 1f, 3, 2, 2).cell)
        assertEquals(132, whoDidItGridMetrics(Int.MAX_VALUE, Int.MAX_VALUE, 3f, 5, 3, 3).cell)
    }

    @Test
    fun `info panel takes a bounded share and leaves the board the larger part`() {
        for (available in 60..1200 step 7) {
            val total = available.dp
            val panel = whoDidItPanelHeight(total)
            assertTrue("$total -> $panel", panel <= 190.dp)
            assertTrue("$total -> $panel", panel <= total * 0.45f)
            if (total >= 267.dp) assertTrue("$total -> $panel", panel >= 120.dp)
        }
        assertEquals(190.dp, whoDidItPanelHeight(700.dp))
    }
}
