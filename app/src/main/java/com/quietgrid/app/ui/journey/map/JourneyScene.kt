package com.quietgrid.app.ui.journey.map

import com.quietgrid.app.ui.theme.ResolvedTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val JOURNEY_SCENE_PAGE_WIDTH = 390f
const val JOURNEY_SCENE_PAGE_HEIGHT = 844f
const val JOURNEY_SCENE_ROAD_CLEARANCE = 60f

enum class JourneySceneKind { SPACE, SKY, SKETCH, ZEN, ARCTIC, CAFE, GOTHIC }

enum class ScenePaint { INK, INK_STRONG, FILL_SOFT, FILL_ACCENT, GLOW, SURFACE, BACKDROP, AURORA_1, AURORA_2, AURORA_3, ROAD, ROAD_FILL, ROAD_DONE }

data class SceneBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun union(other: SceneBounds) = SceneBounds(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))
}

data class SceneItem(
    val pathData: String,
    val fill: ScenePaint?,
    val stroke: ScenePaint?,
    val strokeWidth: Float,
    val dash: List<Float>?,
    val alpha: Float,
    val bounds: SceneBounds,
    val wash: Boolean = false,
)

data class RoadStroke(
    val paint: ScenePaint,
    val width: Float,
    val dash: List<Float>? = null,
    val done: Boolean = false,
    val alpha: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

fun journeySceneFor(theme: ResolvedTheme): JourneySceneKind = when (theme) {
    ResolvedTheme.DARK -> JourneySceneKind.SPACE
    ResolvedTheme.LIGHT -> JourneySceneKind.SKY
    ResolvedTheme.PENCIL -> JourneySceneKind.SKETCH
    ResolvedTheme.SILK -> JourneySceneKind.ZEN
    ResolvedTheme.NORD -> JourneySceneKind.ARCTIC
    ResolvedTheme.COFFEE -> JourneySceneKind.CAFE
    ResolvedTheme.DRACULA -> JourneySceneKind.GOTHIC
}

fun journeySceneSeed(scope: String, chapter: Int): Int = "$scope|$chapter".hashCode()

fun journeyRoadStyle(kind: JourneySceneKind): List<RoadStroke> = when (kind) {
    JourneySceneKind.SPACE -> dottedRoad(2f, listOf(2f, 8f))
    JourneySceneKind.SKY -> dottedRoad(2.5f, listOf(2f, 9f))
    JourneySceneKind.ZEN -> dottedRoad(2f, listOf(1f, 10f))
    JourneySceneKind.SKETCH -> listOf(
        RoadStroke(ScenePaint.ROAD, 1.8f, listOf(7f, 6f)),
        RoadStroke(ScenePaint.INK_STRONG, 1.2f, listOf(11f, 9f), offsetX = 2f, offsetY = 1f),
        RoadStroke(ScenePaint.ROAD_DONE, 2.2f, listOf(7f, 6f), done = true),
    )
    JourneySceneKind.ARCTIC, JourneySceneKind.CAFE, JourneySceneKind.GOTHIC -> listOf(
        RoadStroke(ScenePaint.ROAD, 36f),
        RoadStroke(ScenePaint.ROAD_FILL, 32f),
        RoadStroke(ScenePaint.ROAD, 2f, listOf(8f, 12f)),
        RoadStroke(ScenePaint.ROAD_DONE, 32f, done = true, alpha = 0.22f),
    )
}

private fun dottedRoad(width: Float, dash: List<Float>) = listOf(
    RoadStroke(ScenePaint.ROAD, width, dash),
    RoadStroke(ScenePaint.ROAD_DONE, width, dash, done = true),
)

fun journeyScene(kind: JourneySceneKind, layout: JourneyRoadLayout, seed: Int): List<SceneItem> {
    if (layout.width <= 0f || layout.height <= 0f) return emptyList()
    val scale = layout.width / JOURNEY_SCENE_PAGE_WIDTH
    val pageHeight = JOURNEY_SCENE_PAGE_HEIGHT * scale
    val random = Random(seed)
    val samples = layout.roadSamples()
    val items = mutableListOf<SceneItem>()
    var top = -random.nextFloat() * pageHeight
    var page = 0
    while (top < layout.height) {
        val frame = SceneFrame(scale = scale, width = layout.width, mirror = Random(seed * 31 + page).nextBoolean(), top = top)
        scenePage(kind, frame).forEach { cluster ->
            val solid = cluster.filterNot { it.wash }
            val clear = solid.isEmpty() ||
                layout.distanceToRoad(solid.map { it.bounds }.reduce(SceneBounds::union), samples) >= JOURNEY_SCENE_ROAD_CLEARANCE
            if (clear) items += cluster.filter { it.bounds.bottom > 0f && it.bounds.top < layout.height }
        }
        top += pageHeight
        page++
    }
    return items
}

internal class SceneFrame(val scale: Float, val width: Float, val mirror: Boolean, val top: Float) {
    fun x(value: Float) = if (mirror) width - value * scale else value * scale
    fun y(value: Float) = top + value * scale
}

internal class ScenePath(private val frame: SceneFrame) {
    private val data = StringBuilder()
    private var left = Float.MAX_VALUE
    private var top = Float.MAX_VALUE
    private var right = -Float.MAX_VALUE
    private var bottom = -Float.MAX_VALUE

    private fun point(x: Float, y: Float): String {
        val px = frame.x(x)
        val py = frame.y(y)
        left = minOf(left, px)
        right = maxOf(right, px)
        top = minOf(top, py)
        bottom = maxOf(bottom, py)
        return "${formatPathNumber(px)} ${formatPathNumber(py)}"
    }

    fun move(x: Float, y: Float) = apply { data.append(" M").append(point(x, y)) }
    fun line(x: Float, y: Float) = apply { data.append(" L").append(point(x, y)) }
    fun quad(cx: Float, cy: Float, x: Float, y: Float) = apply { data.append(" Q").append(point(cx, cy)).append(' ').append(point(x, y)) }
    fun cubic(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) =
        apply { data.append(" C").append(point(c1x, c1y)).append(' ').append(point(c2x, c2y)).append(' ').append(point(x, y)) }
    fun close() = apply { data.append(" Z") }

    fun polygon(vararg xy: Float) = apply {
        move(xy[0], xy[1])
        var i = 2
        while (i < xy.size) {
            line(xy[i], xy[i + 1])
            i += 2
        }
        close()
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) = apply {
        val kx = rx * KAPPA
        val ky = ry * KAPPA
        move(cx + rx, cy)
        cubic(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
        cubic(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
        cubic(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        cubic(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
        close()
    }

    fun circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

    fun closedSmooth(points: List<Pair<Float, Float>>) = apply {
        val n = points.size
        move(points[0].first, points[0].second)
        for (i in 0 until n) {
            val a = points[(i - 1 + n) % n]
            val b = points[i]
            val c = points[(i + 1) % n]
            val e = points[(i + 2) % n]
            cubic(
                b.first + (c.first - a.first) / 6, b.second + (c.second - a.second) / 6,
                c.first - (e.first - b.first) / 6, c.second - (e.second - b.second) / 6,
                c.first, c.second,
            )
        }
        close()
    }

    fun item(
        fill: ScenePaint? = null,
        stroke: ScenePaint? = null,
        width: Float = 2f,
        dash: List<Float>? = null,
        alpha: Float = 1f,
        wash: Boolean = false,
    ) = SceneItem(data.toString().trim(), fill, stroke, width, dash, alpha, SceneBounds(left, top, right, bottom), wash)

    private companion object {
        const val KAPPA = 0.5523f
    }
}

internal fun blobPoints(cx: Float, cy: Float, r: Float, k: Float, squash: Float): List<Pair<Float, Float>> = (0 until 14).map { i ->
    val a = i / 14f * 2 * Math.PI.toFloat()
    val rr = r * (1 + 0.13f * sin(3 * a + k) + 0.07f * sin(5 * a + k * 2))
    (cx + rr * cos(a)) to (cy + rr * sin(a) * squash)
}
