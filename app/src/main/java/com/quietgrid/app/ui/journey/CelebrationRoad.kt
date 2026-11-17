package com.quietgrid.app.ui.journey

import com.quietgrid.app.ui.journey.map.JourneyRoadLayout
import com.quietgrid.app.ui.journey.map.RoadPoint
import com.quietgrid.app.ui.journey.map.journeyRoadLayout
import com.quietgrid.app.ui.journey.map.roadSamples
import kotlin.math.hypot

private const val SAMPLES_PER_SEGMENT = 40

data class CelebrationDot(val point: RoadPoint, val fraction: Float)

data class CelebrationRoad(val layout: JourneyRoadLayout, val stopFractions: List<Float>, val dots: List<CelebrationDot>)

fun celebrationRoad(stopCount: Int, width: Float, height: Float, dotsBefore: List<Int> = emptyList()): CelebrationRoad {
    val step = height / (stopCount.coerceAtLeast(0) + 1)
    val layout = journeyRoadLayout(stopCount, width, spacing = step, margin = step)
    if (stopCount <= 0) return CelebrationRoad(layout, emptyList(), emptyList())
    val samples = layout.roadSamples(SAMPLES_PER_SEGMENT)
    val lengths = FloatArray(samples.size)
    for (i in 1 until samples.size) {
        lengths[i] = lengths[i - 1] + hypot(samples[i].x - samples[i - 1].x, samples[i].y - samples[i - 1].y)
    }
    val total = lengths.last().takeIf { it > 0f } ?: 1f
    val stopFractions = List(stopCount) { lengths[(it + 1) * SAMPLES_PER_SEGMENT] / total }
    val dots = dotsBefore.take(stopCount).flatMapIndexed { segment, count ->
        val start = lengths[segment * SAMPLES_PER_SEGMENT]
        val end = lengths[(segment + 1) * SAMPLES_PER_SEGMENT]
        List(count) { j ->
            val target = start + (end - start) * (j + 1) / (count + 1)
            CelebrationDot(pointAt(samples, lengths, target), target / total)
        }
    }
    return CelebrationRoad(layout, stopFractions, dots)
}

private fun pointAt(samples: List<RoadPoint>, lengths: FloatArray, target: Float): RoadPoint {
    val after = lengths.indexOfFirst { it >= target }.let { if (it < 1) 1 else it }
    val from = lengths[after - 1]
    val to = lengths[after]
    val t = if (to > from) (target - from) / (to - from) else 0f
    val a = samples[after - 1]
    val b = samples[after]
    return RoadPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}
