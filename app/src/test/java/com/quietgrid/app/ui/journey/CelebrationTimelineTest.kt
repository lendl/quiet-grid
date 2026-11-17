package com.quietgrid.app.ui.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationTimelineTest {
    private val timeline = CelebrationTimeline(chapterLightMs(12))

    @Test
    fun `the light run takes a short step per stop within fixed bounds`() {
        assertEquals(1800L, chapterLightMs(12))
        assertEquals(CELEBRATION_MIN_LIGHT_MS, chapterLightMs(3))
        assertEquals(CELEBRATION_MAX_LIGHT_MS, chapterLightMs(60))
    }

    @Test
    fun `text, stats and buttons come after the light reaches the top`() {
        assertTrue(timeline.headlineMs > timeline.lightEndMs)
        assertTrue(timeline.sublineMs > timeline.headlineMs)
        assertTrue(timeline.statsMs > timeline.sublineMs)
        assertTrue(timeline.countStartMs > timeline.statsMs)
        assertTrue(timeline.buttonsMs > timeline.statsMs)
        assertTrue(timeline.totalMs >= timeline.countStartMs + CELEBRATION_COUNT_MS)
    }

    @Test
    fun `stops light up in road order and all are lit when the light ends`() {
        assertFalse(timeline.lit(CELEBRATION_LIGHT_START_MS, 0.1f))
        assertTrue(timeline.lit(CELEBRATION_LIGHT_START_MS + timeline.lightMs / 2, 0.4f))
        assertFalse(timeline.lit(CELEBRATION_LIGHT_START_MS + timeline.lightMs / 2, 0.6f))
        assertTrue(timeline.lit(timeline.lightEndMs, 1f))
    }

    @Test
    fun `every count starts at zero and lands on its total`() {
        assertEquals(0, timeline.count(timeline.countStartMs, 152))
        val middle = timeline.count(timeline.countStartMs + CELEBRATION_COUNT_MS / 2, 152)
        assertTrue(middle in 1 until 152)
        assertEquals(152, timeline.count(timeline.countStartMs + CELEBRATION_COUNT_MS, 152))
        assertEquals(29740, timeline.count(timeline.totalMs, 29740))
    }

    @Test
    fun `the ring only shows while it grows`() {
        assertEquals(0f, timeline.ring(timeline.lightEndMs).alpha, 0f)
        assertTrue(timeline.ring(timeline.lightEndMs + CELEBRATION_RING_MS / 2).alpha > 0f)
        assertEquals(0f, timeline.ring(timeline.lightEndMs + CELEBRATION_RING_MS).alpha, 0f)
    }

    @Test
    fun `everything has settled at the end`() {
        val end = timeline.totalMs
        assertEquals(1f, timeline.fade(end), 0f)
        assertEquals(1f, timeline.popScale(end, 0.5f), 0.0001f)
        assertEquals(1f, timeline.popScale(end, 1f), 0.0001f)
        assertEquals(1f, timeline.rays(end), 0f)
        listOf(timeline.headlineMs, timeline.sublineMs, timeline.statsMs).forEach {
            assertEquals(RiseFrame(1f, 0f), riseFrame(end, it))
        }
        assertEquals(RiseFrame(1f, 0f), riseFrame(end, timeline.buttonsMs, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP))
    }
}
