package com.quietgrid.app.ui.journey

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

const val MASTERED_CELEBRATION_TOTAL_MS = 6600L
const val MASTERED_LIGHT_START_MS = 400L
const val MASTERED_LIGHT_SPAN_MS = 3000L
const val MASTERED_NODE_POP_MS = 420L
const val MASTERED_NODE_POP_GROWTH = 0.3f
const val MASTERED_LINE_DELAY_MS = 120L
const val MASTERED_LINE_EXTRA_MS = 60L
const val MASTERED_LINE_ALPHA = 0.75f
const val MASTERED_GOLD_START_MS = 3600L
const val MASTERED_GOLD_DURATION_MS = 1000L
const val MASTERED_GOLD_NODE_STAGGER_MS = 60L
const val MASTERED_GOLD_FILL_ALPHA = 0.10f
const val MASTERED_STAR_START_MS = 4600L
const val MASTERED_STAR_DURATION_MS = 700L
const val MASTERED_STAR_OVERSHOOT = 1.2f
const val MASTERED_STAR_TURN_DEGREES = -30f
const val MASTERED_HALO_START_MS = 4900L
const val MASTERED_HALO_DURATION_MS = 900L
const val MASTERED_HALO_ALPHA = 0.55f
const val MASTERED_HALO_DEGREES_PER_SECOND = 6f
const val MASTERED_HEADLINE_MS = 5200L
const val MASTERED_SUBLINE_MS = 5350L
const val MASTERED_STATS_MS = 5550L
const val MASTERED_NOTE_MS = 5800L
const val MASTERED_PRIMARY_MS = 6100L
const val MASTERED_SECONDARY_MS = 6200L
const val CONSTELLATION_INNER_RATIO = 0.435f
const val CONSTELLATION_WIDTH_RATIO = 0.36f
const val STAR_FIELD_COUNT = 46
const val TWINKLE_PERIOD_DIVISOR_MS = 900f

private const val STAR_FIELD_MIN_RADIUS_DP = 0.6f
private const val STAR_FIELD_RADIUS_RANGE_DP = 1.2f
private const val STAR_FIELD_MIN_ALPHA = 0.15f
private const val STAR_FIELD_ALPHA_RANGE = 0.3f
private const val TWINKLE_FLOOR = 0.6f
private const val TWINKLE_SWING = 0.4f

data class ConstellationNodeFrame(val lit: Boolean, val gold: Boolean, val scale: Float)

data class CenterStarFrame(val scale: Float, val rotationDegrees: Float, val alpha: Float)

data class SkyDot(val x: Float, val y: Float, val radiusDp: Float, val alpha: Float, val phase: Float)

fun masteredStepMs(count: Int): Float = MASTERED_LIGHT_SPAN_MS.toFloat() / count.coerceAtLeast(1)

fun masteredNodeStartMs(index: Int, count: Int): Long = MASTERED_LIGHT_START_MS + (index * masteredStepMs(count)).roundToLong()

fun masteredNodeFrame(elapsedMs: Long, index: Int, count: Int): ConstellationNodeFrame {
    val start = masteredNodeStartMs(index, count)
    val pop = phase(elapsedMs, start, MASTERED_NODE_POP_MS)
    val lit = elapsedMs > start
    val gold = elapsedMs > MASTERED_GOLD_START_MS + index * MASTERED_GOLD_NODE_STAGGER_MS
    val scale = if (lit) 1f + MASTERED_NODE_POP_GROWTH * sin(PI * pop).toFloat() else 1f
    return ConstellationNodeFrame(lit = lit || gold, gold = gold, scale = scale)
}

fun masteredLineProgress(elapsedMs: Long, index: Int, count: Int): Float {
    val start = masteredNodeStartMs(index, count) + MASTERED_LINE_DELAY_MS
    val duration = (masteredStepMs(count) + MASTERED_LINE_EXTRA_MS).roundToLong()
    return easeInOutQuad(phase(elapsedMs, start, duration))
}

fun masteredGoldProgress(elapsedMs: Long): Float =
    easeInOutQuad(phase(elapsedMs, MASTERED_GOLD_START_MS, MASTERED_GOLD_DURATION_MS))

fun masteredLineAlpha(elapsedMs: Long): Float = MASTERED_LINE_ALPHA * (1f - masteredGoldProgress(elapsedMs))

fun masteredCenterStarFrame(elapsedMs: Long): CenterStarFrame {
    val progress = phase(elapsedMs, MASTERED_STAR_START_MS, MASTERED_STAR_DURATION_MS)
    return CenterStarFrame(
        scale = easeOutBack(progress, MASTERED_STAR_OVERSHOOT),
        rotationDegrees = MASTERED_STAR_TURN_DEGREES * (1f - easeOutCubic(progress)),
        alpha = min(1f, 3f * progress),
    )
}

fun masteredHaloAlpha(elapsedMs: Long): Float =
    MASTERED_HALO_ALPHA * easeOutCubic(phase(elapsedMs, MASTERED_HALO_START_MS, MASTERED_HALO_DURATION_MS))

fun constellationOuterRadius(width: Float, maxRadius: Float): Float = min(maxRadius, CONSTELLATION_WIDTH_RATIO * width)

fun constellationOutline(center: Offset, outerRadius: Float): List<Offset> =
    starPolygon(center, outerRadius, CONSTELLATION_INNER_RATIO)

fun outlineLength(outline: List<Offset>): Float =
    outline.indices.sumOf { (outline[(it + 1) % outline.size] - outline[it]).getDistance().toDouble() }.toFloat()

fun constellationNodes(outline: List<Offset>, count: Int): List<Offset> {
    if (outline.isEmpty() || count <= 0) return emptyList()
    val edges = outline.indices.map { outline[it] to outline[(it + 1) % outline.size] }
    val perimeter = outlineLength(outline)
    return List(count) { node ->
        var remaining = perimeter * node / count
        var placed: Offset? = null
        for ((from, to) in edges) {
            val length = (to - from).getDistance()
            if (remaining <= length || length == 0f) {
                val t = if (length == 0f) 0f else (remaining / length).coerceIn(0f, 1f)
                placed = from + (to - from) * t
                break
            }
            remaining -= length
        }
        placed ?: outline.first()
    }
}

fun starField(seed: Int, count: Int = STAR_FIELD_COUNT): List<SkyDot> {
    val random = Random(seed)
    return List(count) {
        SkyDot(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radiusDp = STAR_FIELD_MIN_RADIUS_DP + random.nextFloat() * STAR_FIELD_RADIUS_RANGE_DP,
            alpha = STAR_FIELD_MIN_ALPHA + random.nextFloat() * STAR_FIELD_ALPHA_RANGE,
            phase = random.nextFloat() * (2 * PI).toFloat(),
        )
    }
}

fun twinkleAlpha(dot: SkyDot, twinkleRadians: Float?): Float =
    if (twinkleRadians == null) dot.alpha else dot.alpha * (TWINKLE_FLOOR + TWINKLE_SWING * sin(twinkleRadians + dot.phase))
