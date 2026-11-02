package com.quietgrid.engine.journey

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRatingTest {
    private val scale = JourneyRatingScale(listOf(RatingPoint(0, 100), RatingPoint(10, 200), RatingPoint(20, 1000)))

    @Test
    fun `rate interpolates linearly between points`() {
        assertEquals(150, scale.rate(5))
        assertEquals(600, scale.rate(15))
        assertEquals(200, scale.rate(10))
    }

    @Test
    fun `rate clamps outside the table`() {
        assertEquals(100, scale.rate(-3))
        assertEquals(1000, scale.rate(99))
    }

    @Test
    fun `rate with a single point returns its rating`() {
        val single = JourneyRatingScale(listOf(RatingPoint(7, 550)))
        assertEquals(550, single.rate(0))
        assertEquals(550, single.rate(100))
    }

    @Test
    fun `tiered raw orders tiers before fine`() {
        assertTrue(tieredRawScore("easy", JOURNEY_TIER_SPAN * 5) < tieredRawScore("medium", 0))
        assertEquals(2 * JOURNEY_TIER_SPAN, tieredRawScore("hard", -4))
    }

    @Test
    fun `pack raw score orders slots left to right and clamps each slot`() {
        assertEquals(345L, packRawScore(3 to 1, 45 to 2))
        assertEquals(799L, packRawScore(7 to 1, 123 to 2))
        assertEquals(100L, packRawScore(1 to 1, -5 to 2))
        assertTrue(packRawScore(1 to 1, 0 to 2) > packRawScore(0 to 1, 99 to 2))
    }

    @Test
    fun `pack raw score rejects more digits than a tier holds`() {
        assertThrows(IllegalArgumentException::class.java) { packRawScore(1 to 8, 1 to 8) }
    }

    @Test
    fun `tiered raw rejects unknown labels`() {
        assertThrows(IllegalArgumentException::class.java) { tieredRawScore("legendary", 0) }
    }

    @Test
    fun `journey stop omits null replaces when encoded`() {
        assertFalse("replaces" in Json.encodeToString(JourneyStop("a", "easy", 120, 90)))
    }
}
