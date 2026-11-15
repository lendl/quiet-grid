package com.quietgrid.app.ui.journey

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class ChapterCelebrationTimingTest {
    private val width = 342f
    private val inset = 24f
    private val rowGap = 54f
    private val top = 16f
    private val spacing = (width - 2 * inset) / 9

    @Test
    fun `sixty stops lay out in six rows that turn back and forth`() {
        val layout = chapterRoadLayout(60, width, inset, rowGap, top)
        assertEquals(6, layout.rowCount)
        assertEquals(Offset(inset, top), layout.points[0])
        assertEquals(width - inset, layout.points[9].x, 0.01f)
        assertEquals(width - inset, layout.points[10].x, 0.01f)
        assertEquals(top + rowGap, layout.points[10].y, 0.01f)
        assertEquals(inset, layout.points[19].x, 0.01f)
        assertEquals(top + 5 * rowGap, layout.points[59].y, 0.01f)
    }

    @Test
    fun `fractions run from 0 to 1 and count the turns`() {
        val layout = chapterRoadLayout(60, width, inset, rowGap, top)
        assertEquals(0f, layout.fractions.first(), 0f)
        assertEquals(1f, layout.fractions.last(), 0.0001f)
        assertTrue(layout.fractions.zipWithNext().all { (a, b) -> b > a })
        val turn = (PI * rowGap / 2).toFloat()
        val total = 5 * (9 * spacing + turn) + 9 * spacing
        assertEquals((9 * spacing + turn) / total, layout.fractions[10], 0.0001f)
    }

    @Test
    fun `a partial last row ends at its last stop`() {
        val layout = chapterRoadLayout(27, width, inset, rowGap, top)
        assertEquals(3, layout.rowCount)
        assertEquals(27, layout.points.size)
        assertEquals(inset + 6 * spacing, layout.points[26].x, 0.01f)
        assertEquals(1f, layout.fractions.last(), 0.0001f)
    }

    @Test
    fun `road draws from 300 to 2700 ms`() {
        assertEquals(0f, chapterRoadProgress(0), 0f)
        assertEquals(0f, chapterRoadProgress(CHAPTER_ROAD_START_MS), 0f)
        assertEquals(0.5f, chapterRoadProgress(CHAPTER_ROAD_START_MS + CHAPTER_ROAD_DURATION_MS / 2), 0.0001f)
        assertEquals(1f, chapterRoadProgress(CHAPTER_ROAD_START_MS + CHAPTER_ROAD_DURATION_MS), 0f)
    }

    @Test
    fun `a stop lights when the road reaches it and pops once`() {
        val fraction = 0.3f
        val litAt = chapterStopLitMs(fraction)
        assertFalse(chapterStopLit(litAt - 20, fraction))
        assertTrue(chapterStopLit(litAt + 1, fraction))
        val peak = (0..CHAPTER_STOP_POP_MS.toInt()).maxOf { chapterStopScale(litAt + it, fraction) }
        assertEquals(1f + CHAPTER_STOP_POP_GROWTH, peak, 0.01f)
        assertEquals(1f, chapterStopScale(litAt + CHAPTER_STOP_POP_MS + 50, fraction), 0.0001f)
        assertEquals(1f, chapterStopScale(litAt - 50, fraction), 0f)
    }

    @Test
    fun `ring opens once at the end and then disappears`() {
        assertEquals(0f, chapterRingFrame(CHAPTER_RING_START_MS).alpha, 0f)
        val middle = chapterRingFrame(CHAPTER_RING_START_MS + CHAPTER_RING_DURATION_MS / 2)
        assertEquals(CHAPTER_RING_ALPHA / 2, middle.alpha, 0.01f)
        assertTrue(middle.radiusDp > CHAPTER_RING_FROM_DP)
        val end = chapterRingFrame(CHAPTER_RING_START_MS + CHAPTER_RING_DURATION_MS)
        assertEquals(0f, end.alpha, 0f)
        assertEquals(CHAPTER_RING_FROM_DP + CHAPTER_RING_GROWTH_DP, end.radiusDp, 0.0001f)
    }

    @Test
    fun `star count ticks up to the chapter total`() {
        assertEquals(0, chapterStarCount(CHAPTER_COUNT_START_MS, 142))
        val middle = chapterStarCount(CHAPTER_COUNT_START_MS + CHAPTER_COUNT_DURATION_MS / 2, 142)
        assertTrue(middle in 1 until 142)
        assertEquals(142, chapterStarCount(CHAPTER_CELEBRATION_TOTAL_MS, 142))
    }

    @Test
    fun `everything has settled by the total time`() {
        val t = CHAPTER_CELEBRATION_TOTAL_MS
        listOf(CHAPTER_HEADLINE_MS, CHAPTER_SUBLINE_MS, CHAPTER_STATS_MS, CHAPTER_NOTE_MS).forEach {
            assertEquals(RiseFrame(1f, 0f), riseFrame(t, it))
        }
        listOf(CHAPTER_PRIMARY_MS, CHAPTER_SECONDARY_MS).forEach {
            assertEquals(RiseFrame(1f, 0f), riseFrame(t, it, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP))
        }
        assertTrue(chapterStopLit(t, 1f))
    }

    @Test
    fun `rise starts hidden and lowered`() {
        assertEquals(RiseFrame(0f, CELEBRATION_RISE_DP), riseFrame(CHAPTER_HEADLINE_MS, CHAPTER_HEADLINE_MS))
    }

    @Test
    fun `inverse ease undoes the ease`() {
        listOf(0f, 0.1f, 0.4f, 0.5f, 0.8f, 1f).forEach {
            assertEquals(it, easeInOutQuad(inverseEaseInOutQuad(it)), 0.0001f)
        }
    }
}
