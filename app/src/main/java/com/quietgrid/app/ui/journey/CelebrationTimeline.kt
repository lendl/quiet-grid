package com.quietgrid.app.ui.journey

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

const val CELEBRATION_OPEN_DELAY_MS = 600L
const val CELEBRATION_FADE_MS = 350L
const val CELEBRATION_LIGHT_START_MS = 450L
const val CELEBRATION_STEP_MS = 150L
const val CELEBRATION_MIN_LIGHT_MS = 1200L
const val CELEBRATION_MAX_LIGHT_MS = 2500L
const val CELEBRATION_JOURNEY_LIGHT_MS = 2400L
const val CELEBRATION_POP_MS = 320L
const val CELEBRATION_POP_GROWTH = 0.35f
const val CELEBRATION_RING_MS = 1100L
const val CELEBRATION_RING_GROWTH_DP = 40f
const val CELEBRATION_RING_ALPHA = 0.6f
const val CELEBRATION_RAYS_MS = 450L
const val CELEBRATION_COUNT_MS = 1000L

private const val HEADLINE_AFTER_LIGHT_MS = 100L
private const val SUBLINE_GAP_MS = 120L
private const val STATS_GAP_MS = 150L
private const val COUNT_GAP_MS = 100L
private const val BUTTONS_GAP_MS = 250L
private const val LIT_TOLERANCE = 0.0005f

data class RingFrame(val growthDp: Float, val alpha: Float)

fun chapterLightMs(stopCount: Int): Long =
    (stopCount * CELEBRATION_STEP_MS).coerceIn(CELEBRATION_MIN_LIGHT_MS, CELEBRATION_MAX_LIGHT_MS)

data class CelebrationTimeline(val lightMs: Long) {
    val lightEndMs: Long get() = CELEBRATION_LIGHT_START_MS + lightMs
    val headlineMs: Long get() = lightEndMs + HEADLINE_AFTER_LIGHT_MS
    val sublineMs: Long get() = headlineMs + SUBLINE_GAP_MS
    val statsMs: Long get() = sublineMs + STATS_GAP_MS
    val countStartMs: Long get() = statsMs + COUNT_GAP_MS
    val buttonsMs: Long get() = statsMs + BUTTONS_GAP_MS
    val totalMs: Long get() = maxOf(countStartMs + CELEBRATION_COUNT_MS, lightEndMs + CELEBRATION_RING_MS)

    fun fade(elapsedMs: Long): Float = phase(elapsedMs, 0L, CELEBRATION_FADE_MS)

    fun lightProgress(elapsedMs: Long): Float = phase(elapsedMs, CELEBRATION_LIGHT_START_MS, lightMs)

    fun lit(elapsedMs: Long, fraction: Float): Boolean = lightProgress(elapsedMs) >= fraction - LIT_TOLERANCE

    fun popScale(elapsedMs: Long, fraction: Float): Float {
        if (!lit(elapsedMs, fraction)) return 1f
        val litAt = CELEBRATION_LIGHT_START_MS + (lightMs * fraction).roundToLong()
        return 1f + CELEBRATION_POP_GROWTH * sin(PI * phase(elapsedMs, litAt, CELEBRATION_POP_MS)).toFloat()
    }

    fun ring(elapsedMs: Long): RingFrame {
        val progress = phase(elapsedMs, lightEndMs, CELEBRATION_RING_MS)
        val alpha = if (progress > 0f && progress < 1f) CELEBRATION_RING_ALPHA * (1f - progress) else 0f
        return RingFrame(CELEBRATION_RING_GROWTH_DP * easeOutCubic(progress), alpha)
    }

    fun rays(elapsedMs: Long): Float = easeOutCubic(phase(elapsedMs, lightEndMs, CELEBRATION_RAYS_MS))

    fun count(elapsedMs: Long, total: Int): Int =
        (total * easeOutCubic(phase(elapsedMs, countStartMs, CELEBRATION_COUNT_MS))).roundToInt()
}
