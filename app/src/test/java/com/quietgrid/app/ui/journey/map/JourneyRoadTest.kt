package com.quietgrid.app.ui.journey.map

import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.JourneyStopState
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRoadTest {
    @Test
    fun `stops climb a fixed spacing apart from the bottom`() {
        val layout = journeyRoadLayout(stopCount = 25, width = 390f)
        assertEquals(25, layout.stops.size)
        assertEquals(layout.height - JOURNEY_ROAD_MARGIN, layout.stops.first().y, 0.01f)
        layout.stops.zipWithNext().forEach { (below, above) ->
            assertEquals(JOURNEY_STOP_SPACING, below.y - above.y, 0.01f)
        }
        assertEquals(JOURNEY_ROAD_MARGIN, layout.stops.last().y, 0.01f)
    }

    @Test
    fun `x stays inside the width with the stop radius`() {
        listOf(200f, 300f, 360f, 390f, 480f, 800f).forEach { width ->
            val layout = journeyRoadLayout(stopCount = 30, width = width)
            layout.stops.forEach { stop ->
                assertTrue("width $width x ${stop.x}", stop.x - 30f >= 0f && stop.x + 30f <= width)
            }
        }
    }

    @Test
    fun `amplitude is capped on wide panes`() {
        val layout = journeyRoadLayout(stopCount = 30, width = 1280f)
        val centre = 640f
        layout.stops.forEach { assertTrue(kotlin.math.abs(it.x - centre) <= JOURNEY_ROAD_MAX_AMPLITUDE + 0.01f) }
        val narrow = journeyRoadLayout(stopCount = 30, width = 390f)
        val swing = narrow.stops.maxOf { kotlin.math.abs(it.x - 195f) }
        assertEquals(95f, swing, 2f)
    }

    @Test
    fun `road leads in below the first stop and ends at the last stop`() {
        val layout = journeyRoadLayout(stopCount = 25, width = 390f)
        assertEquals(26, layout.full.size)
        assertEquals(layout.stops, layout.full.subList(1, 26))
        assertTrue(layout.full.first().y > layout.stops.first().y)
        assertEquals(layout.stops.last(), layout.full.last())
        assertTrue(layout.full.first().y <= layout.height)
    }

    @Test
    fun `smooth path starts at the first point and has one curve per segment`() {
        val points = listOf(RoadPoint(10f, 100f), RoadPoint(50f, 60f), RoadPoint(20f, 20f), RoadPoint(40f, 0f))
        val path = smoothRoadPath(points)
        assertTrue(path, path.startsWith("M10.0 100.0"))
        assertEquals(3, path.count { it == 'C' })
        assertTrue(path, path.endsWith("40.0 0.0"))
        assertEquals(2, smoothRoadPath(points, segments = 2).count { it == 'C' })
        assertTrue(smoothRoadPath(points).startsWith(smoothRoadPath(points, segments = 2)))
    }

    @Test
    fun `done count stops at the current stop`() {
        val states = listOf(JourneyStopState.DONE, JourneyStopState.SKIPPED, JourneyStopState.CURRENT, JourneyStopState.DONE, JourneyStopState.OPEN)
        assertEquals(2, doneStopCount(rows(states)))
        assertEquals(3, doneStopCount(rows(List(3) { JourneyStopState.DONE })))
        assertEquals(0, doneStopCount(rows(listOf(JourneyStopState.CURRENT, JourneyStopState.OPEN))))
    }

    private fun rows(states: List<JourneyStopState>) = states.mapIndexed { i, state ->
        JourneyStopRow(1, i + 1, JourneyStop("s$i", "easy", i, 60), JourneyStopProgress(), state)
    }
}
