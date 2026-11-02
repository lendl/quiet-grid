package com.quietgrid.app.ui.journey.map

import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.JourneyStopState
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val JOURNEY_STOP_SPACING = 72f
const val JOURNEY_ROAD_MARGIN = 96f
const val JOURNEY_ROAD_AMPLITUDE_FRACTION = 95f / 390f
const val JOURNEY_ROAD_MAX_AMPLITUDE = 140f
const val JOURNEY_STOP_HALF_SLOT = 48f

data class RoadPoint(val x: Float, val y: Float)

data class JourneyRoadLayout(val width: Float, val height: Float, val stops: List<RoadPoint>, val full: List<RoadPoint>)

fun journeyRoadLayout(stopCount: Int, width: Float): JourneyRoadLayout {
    val height = (stopCount - 1).coerceAtLeast(0) * JOURNEY_STOP_SPACING + 2 * JOURNEY_ROAD_MARGIN
    val centre = width / 2
    val amplitude = minOf(width * JOURNEY_ROAD_AMPLITUDE_FRACTION, JOURNEY_ROAD_MAX_AMPLITUDE, centre - JOURNEY_STOP_HALF_SLOT)
        .coerceAtLeast(0f)
    fun point(i: Int) = RoadPoint(
        x = centre + amplitude * sin(i * 0.95f + 0.4f),
        y = height - JOURNEY_ROAD_MARGIN - i * JOURNEY_STOP_SPACING,
    )
    val stops = (0 until stopCount).map(::point)
    return JourneyRoadLayout(width, height, stops, listOf(point(-1)) + stops + point(stopCount))
}

fun smoothRoadPath(points: List<RoadPoint>, segments: Int = points.size - 1): String {
    if (points.isEmpty()) return ""
    val out = StringBuilder("M${formatPathNumber(points[0].x)} ${formatPathNumber(points[0].y)}")
    val last = points.lastIndex
    for (i in 0 until segments.coerceAtMost(last)) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(last)]
        val c1x = p1.x + (p2.x - p0.x) / 6
        val c1y = p1.y + (p2.y - p0.y) / 6
        val c2x = p2.x - (p3.x - p1.x) / 6
        val c2y = p2.y - (p3.y - p1.y) / 6
        out.append(" C${formatPathNumber(c1x)} ${formatPathNumber(c1y)} ${formatPathNumber(c2x)} ${formatPathNumber(c2y)} ${formatPathNumber(p2.x)} ${formatPathNumber(p2.y)}")
    }
    return out.toString()
}

fun JourneyRoadLayout.roadSamples(perSegment: Int = 20): List<RoadPoint> {
    val points = full
    if (points.size < 2) return points
    val last = points.lastIndex
    val samples = ArrayList<RoadPoint>(last * perSegment + 1)
    samples += points[0]
    for (i in 0 until last) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(last)]
        val c1 = RoadPoint(p1.x + (p2.x - p0.x) / 6, p1.y + (p2.y - p0.y) / 6)
        val c2 = RoadPoint(p2.x - (p3.x - p1.x) / 6, p2.y - (p3.y - p1.y) / 6)
        for (step in 1..perSegment) {
            val t = step.toFloat() / perSegment
            val u = 1 - t
            val a = u * u * u
            val b = 3 * u * u * t
            val c = 3 * u * t * t
            val d = t * t * t
            samples += RoadPoint(a * p1.x + b * c1.x + c * c2.x + d * p2.x, a * p1.y + b * c1.y + c * c2.y + d * p2.y)
        }
    }
    return samples
}

fun JourneyRoadLayout.distanceToRoad(bounds: SceneBounds, samples: List<RoadPoint> = roadSamples()): Float =
    samples.minOfOrNull { point ->
        val dx = maxOf(bounds.left - point.x, 0f, point.x - bounds.right)
        val dy = maxOf(bounds.top - point.y, 0f, point.y - bounds.bottom)
        sqrt(dx * dx + dy * dy)
    } ?: Float.MAX_VALUE

fun doneStopCount(rows: List<JourneyStopRow>): Int =
    rows.takeWhile { it.state == JourneyStopState.DONE || it.state == JourneyStopState.SKIPPED }.size

internal fun formatPathNumber(value: Float): String = ((value * 10).roundToInt() / 10f).toString()
