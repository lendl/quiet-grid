package com.quietgrid.cli.journey

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.RatingPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JourneyScalesTest {
    @Test
    fun `scale from distinct raws maps p0 to 100 and p100 to 1000`() {
        val points = buildRatingScale((0L..1000L).toList())
        assertEquals(101, points.size)
        assertEquals(RatingPoint(0, 100), points.first())
        assertEquals(RatingPoint(1000, 1000), points.last())
    }

    @Test
    fun `ties collapse into strictly increasing points`() {
        val points = buildRatingScale(List(70) { 5L } + (1L..30L))
        points.zipWithNext().forEach { (a, b) ->
            assertTrue(a.raw < b.raw)
            assertTrue(a.rating < b.rating)
        }
        val rating = JourneyRatingScale(points).rate(5)
        assertTrue(rating in 101..999)
    }

    @Test
    fun `rating is monotone over a tied bank`() {
        val scale = JourneyRatingScale(buildRatingScale(List(70) { 5L } + (1L..30L)))
        (0L..40L).zipWithNext().forEach { (a, b) -> assertTrue(scale.rate(a) <= scale.rate(b)) }
    }

    @Test
    fun `target seconds is linear and rounded to ten`() {
        val target = JourneyTargetSeconds(120, 1200)
        assertEquals(120, targetSecondsFor(100, target))
        assertEquals(1200, targetSecondsFor(1000, target))
        assertEquals(660, targetSecondsFor(550, target))
        assertEquals(120, targetSecondsFor(101, target))
    }

    @Test
    fun `scales round-trip through the file`() {
        val file = File.createTempFile("journey_scales", ".json").apply { deleteOnExit() }
        val scales = mapOf(
            "b" to JourneyGameScale(JourneyTargetSeconds(60, 900), listOf(RatingPoint(1, 100), RatingPoint(9, 1000))),
            "a" to JourneyGameScale(JourneyTargetSeconds(120, 1200)),
        )
        writeJourneyScales(file.path, scales)
        assertEquals(scales, readJourneyScales(file.path))
    }

    @Test
    fun `committed scales file lists the journey games`() {
        assertEquals(JOURNEY_GAMES.toSet(), readJourneyScales("cli/src/main/resources/journey_rating_scales.json").keys)
    }
}
