package com.quietgrid.app.ui.journey

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

const val CHAPTER_CELEBRATION_TOTAL_MS = 4600L
const val CHAPTER_ROAD_START_MS = 300L
const val CHAPTER_ROAD_DURATION_MS = 2400L
const val CHAPTER_STOP_POP_MS = 320L
const val CHAPTER_STOP_POP_GROWTH = 0.35f
const val CHAPTER_RING_START_MS = 2700L
const val CHAPTER_RING_DURATION_MS = 1100L
const val CHAPTER_RING_FROM_DP = 11f
const val CHAPTER_RING_GROWTH_DP = 40f
const val CHAPTER_RING_ALPHA = 0.6f
const val CHAPTER_HEADLINE_MS = 2900L
const val CHAPTER_SUBLINE_MS = 3050L
const val CHAPTER_STATS_MS = 3250L
const val CHAPTER_COUNT_START_MS = 3400L
const val CHAPTER_COUNT_DURATION_MS = 900L
const val CHAPTER_NOTE_MS = 3600L
const val CHAPTER_PRIMARY_MS = 4000L
const val CHAPTER_SECONDARY_MS = 4100L
const val CHAPTER_ROAD_STOPS_PER_ROW = 10

private const val LIT_TOLERANCE = 0.0005f

data class RingFrame(val radiusDp: Float, val alpha: Float)

data class ChapterRoadLayout(val points: List<Offset>, val fractions: List<Float>, val rowCount: Int)

fun chapterRoadLayout(stopCount: Int, width: Float, inset: Float, rowGap: Float, top: Float): ChapterRoadLayout {
    if (stopCount <= 0) return ChapterRoadLayout(emptyList(), emptyList(), 0)
    val spacing = (width - 2 * inset) / (CHAPTER_ROAD_STOPS_PER_ROW - 1)
    val turn = (PI * rowGap / 2).toFloat()
    val rowLength = spacing * (CHAPTER_ROAD_STOPS_PER_ROW - 1)
    val points = List(stopCount) { i ->
        val row = i / CHAPTER_ROAD_STOPS_PER_ROW
        val col = i % CHAPTER_ROAD_STOPS_PER_ROW
        val x = if (row % 2 == 0) inset + col * spacing else width - inset - col * spacing
        Offset(x, top + row * rowGap)
    }
    val distances = List(stopCount) { i ->
        val row = i / CHAPTER_ROAD_STOPS_PER_ROW
        val col = i % CHAPTER_ROAD_STOPS_PER_ROW
        row * (rowLength + turn) + col * spacing
    }
    val total = distances.last()
    val fractions = distances.map { if (total > 0f) it / total else 0f }
    return ChapterRoadLayout(points, fractions, (stopCount - 1) / CHAPTER_ROAD_STOPS_PER_ROW + 1)
}

fun chapterRoadProgress(elapsedMs: Long): Float =
    easeInOutQuad(phase(elapsedMs, CHAPTER_ROAD_START_MS, CHAPTER_ROAD_DURATION_MS))

fun chapterStopLit(elapsedMs: Long, fraction: Float): Boolean = chapterRoadProgress(elapsedMs) >= fraction - LIT_TOLERANCE

fun chapterStopLitMs(fraction: Float): Long =
    CHAPTER_ROAD_START_MS + (CHAPTER_ROAD_DURATION_MS * inverseEaseInOutQuad(fraction)).roundToLong()

fun chapterStopScale(elapsedMs: Long, fraction: Float): Float {
    if (!chapterStopLit(elapsedMs, fraction)) return 1f
    val age = phase(elapsedMs, chapterStopLitMs(fraction), CHAPTER_STOP_POP_MS)
    return 1f + CHAPTER_STOP_POP_GROWTH * sin(PI * age).toFloat()
}

fun chapterRingFrame(elapsedMs: Long): RingFrame {
    val progress = phase(elapsedMs, CHAPTER_RING_START_MS, CHAPTER_RING_DURATION_MS)
    val alpha = if (progress > 0f && progress < 1f) CHAPTER_RING_ALPHA * (1f - progress) else 0f
    return RingFrame(CHAPTER_RING_FROM_DP + CHAPTER_RING_GROWTH_DP * easeOutCubic(progress), alpha)
}

fun chapterStarCount(elapsedMs: Long, total: Int): Int =
    (total * easeOutCubic(phase(elapsedMs, CHAPTER_COUNT_START_MS, CHAPTER_COUNT_DURATION_MS))).roundToInt()
