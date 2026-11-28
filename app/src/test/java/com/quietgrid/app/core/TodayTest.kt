package com.quietgrid.app.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-26T23:30:00Z")
    }

    @Test
    fun `today follows the zone ahead of utc`() {
        assertEquals(LocalDate(2026, 11, 27), today(fixedClock, TimeZone.of("Europe/Amsterdam")))
    }

    @Test
    fun `today follows the zone behind utc`() {
        assertEquals(LocalDate(2026, 11, 26), today(fixedClock, TimeZone.of("America/New_York")))
    }
}
