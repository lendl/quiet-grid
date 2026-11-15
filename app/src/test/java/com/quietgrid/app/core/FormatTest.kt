package com.quietgrid.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `elapsed time pads seconds`() {
        assertEquals("4:07", formatElapsed(247))
    }

    @Test
    fun `total time under an hour reads as minutes and seconds`() {
        assertEquals("0:00", formatTotalTime(0))
        assertEquals("59:59", formatTotalTime(3599))
    }

    @Test
    fun `total time of an hour or more reads as hours and minutes`() {
        assertEquals("1h 00m", formatTotalTime(3600))
        assertEquals("3h 05m", formatTotalTime(3 * 3600 + 5 * 60 + 59))
        assertEquals("52h 40m", formatTotalTime(52 * 3600 + 40 * 60))
    }
}
