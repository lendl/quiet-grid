package com.quietgrid.app.ui.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationRoadTest {
    @Test
    fun `stops fit inside the box from bottom to top`() {
        val road = celebrationRoad(12, width = 390f, height = 440f)

        assertEquals(12, road.layout.stops.size)
        assertEquals(440f, road.layout.height, 0.01f)
        assertTrue(road.layout.stops.all { it.y > 0f && it.y < 440f })
        assertTrue(road.layout.stops.zipWithNext().all { (a, b) -> b.y < a.y })
    }

    @Test
    fun `stop fractions climb to the end of the road`() {
        val road = celebrationRoad(12, width = 390f, height = 440f)

        assertTrue(road.stopFractions.first() > 0f)
        assertTrue(road.stopFractions.zipWithNext().all { (a, b) -> b > a })
        assertEquals(1f, road.stopFractions.last(), 0.0001f)
    }

    @Test
    fun `dots sit on the road between the chapter they lead up to and the one before`() {
        val road = celebrationRoad(3, width = 390f, height = 440f, dotsBefore = listOf(4, 2, 5))

        assertEquals(11, road.dots.size)
        val firstChapterDots = road.dots.take(4)
        val secondChapterDots = road.dots.drop(4).take(2)
        assertTrue(firstChapterDots.all { it.fraction < road.stopFractions[0] })
        assertTrue(secondChapterDots.all { it.fraction > road.stopFractions[0] && it.fraction < road.stopFractions[1] })
        assertTrue(road.dots.zipWithNext().all { (a, b) -> b.fraction > a.fraction })
    }

    @Test
    fun `a long chapter still fits the same box`() {
        val road = celebrationRoad(60, width = 390f, height = 440f)

        assertEquals(60, road.layout.stops.size)
        assertTrue(road.layout.stops.all { it.y > 0f && it.y < 440f })
    }

    @Test
    fun `no stops gives an empty road`() {
        val road = celebrationRoad(0, width = 390f, height = 440f)

        assertTrue(road.layout.stops.isEmpty())
        assertTrue(road.stopFractions.isEmpty())
        assertTrue(road.dots.isEmpty())
    }
}
