package com.quietgrid.app.ui.journey

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val CELEBRATION_RISE_MS = 520L
const val CELEBRATION_RISE_DP = 14f
const val CELEBRATION_BUTTON_RISE_MS = 500L
const val CELEBRATION_BUTTON_RISE_DP = 10f

data class RiseFrame(val alpha: Float, val offsetDp: Float)

fun phase(elapsedMs: Long, startMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) {
        if (elapsedMs >= startMs) 1f else 0f
    } else {
        ((elapsedMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
    }

fun easeInOutQuad(x: Float): Float = if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2) / 2f

fun inverseEaseInOutQuad(y: Float): Float {
    val clamped = y.coerceIn(0f, 1f)
    return if (clamped < 0.5f) sqrt(clamped / 2f) else 1f - sqrt((1f - clamped) / 2f)
}

fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)

fun easeOutBack(x: Float, overshoot: Float): Float =
    if (x <= 0f) 0f else 1f + (overshoot + 1f) * (x - 1f).pow(3) + overshoot * (x - 1f).pow(2)

fun riseFrame(
    elapsedMs: Long,
    startMs: Long,
    durationMs: Long = CELEBRATION_RISE_MS,
    distanceDp: Float = CELEBRATION_RISE_DP,
): RiseFrame {
    val shown = easeOutCubic(phase(elapsedMs, startMs, durationMs))
    return RiseFrame(alpha = shown, offsetDp = (1f - shown) * distanceDp)
}

fun starPolygon(center: Offset, outerRadius: Float, innerRatio: Float): List<Offset> = List(10) { k ->
    val angle = -PI / 2 + k * PI / 5
    val radius = if (k % 2 == 0) outerRadius else outerRadius * innerRatio
    Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
}
