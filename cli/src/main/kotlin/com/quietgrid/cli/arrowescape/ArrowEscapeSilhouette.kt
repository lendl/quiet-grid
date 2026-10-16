package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_ESCAPE_MASK_ACTIVE
import com.quietgrid.engine.arrowescape.ARROW_ESCAPE_MASK_VOID
import com.quietgrid.engine.arrowescape.CellCoord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
sealed class SilhouetteShape {
    abstract val cut: Boolean
    abstract fun contains(x: Double, y: Double): Boolean
}

@Serializable
@SerialName("circle")
data class SilhouetteCircle(
    val x: Double,
    val y: Double,
    val r: Double,
    override val cut: Boolean = false,
) : SilhouetteShape() {
    override fun contains(x: Double, y: Double): Boolean {
        val dx = x - this.x
        val dy = y - this.y
        return dx * dx + dy * dy <= r * r
    }
}

@Serializable
@SerialName("ellipse")
data class SilhouetteEllipse(
    val x: Double,
    val y: Double,
    val rx: Double,
    val ry: Double,
    override val cut: Boolean = false,
) : SilhouetteShape() {
    override fun contains(x: Double, y: Double): Boolean {
        val nx = (x - this.x) / rx
        val ny = (y - this.y) / ry
        return nx * nx + ny * ny <= 1.0
    }
}

@Serializable
@SerialName("capsule")
data class SilhouetteCapsule(
    val points: List<List<Double>>,
    val width: Double,
    override val cut: Boolean = false,
) : SilhouetteShape() {
    override fun contains(x: Double, y: Double): Boolean {
        val halfSquared = (width / 2) * (width / 2)
        if (points.size == 1) return squaredDistanceToSegment(x, y, points[0], points[0]) <= halfSquared
        return points.zipWithNext().any { (a, b) -> squaredDistanceToSegment(x, y, a, b) <= halfSquared }
    }
}

@Serializable
@SerialName("polygon")
data class SilhouettePolygon(
    val points: List<List<Double>>,
    override val cut: Boolean = false,
) : SilhouetteShape() {
    override fun contains(x: Double, y: Double): Boolean {
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val (xi, yi) = points[i]
            val (xj, yj) = points[j]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }
}

private fun squaredDistanceToSegment(px: Double, py: Double, a: List<Double>, b: List<Double>): Double {
    val dx = b[0] - a[0]
    val dy = b[1] - a[1]
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0.0) 0.0 else (((px - a[0]) * dx + (py - a[1]) * dy) / lengthSquared).coerceIn(0.0, 1.0)
    val qx = a[0] + t * dx - px
    val qy = a[1] + t * dy - py
    return qx * qx + qy * qy
}

@Serializable
data class ArrowEscapeSilhouette(
    val id: String,
    val name: String,
    val width: Double,
    val height: Double,
    val minRows: Int,
    val shapes: List<SilhouetteShape>,
    val parts: Int = 1,
) {
    fun covers(x: Double, y: Double): Boolean {
        var inside = false
        for (shape in shapes) {
            if (shape.cut) {
                if (inside && shape.contains(x, y)) inside = false
            } else if (!inside && shape.contains(x, y)) {
                inside = true
            }
        }
        return inside
    }

    fun colsForRows(rows: Int): Int = max(1, (rows * width / height).roundToInt())
}

data class ArrowEscapeBoardMask(val rows: Int, val cols: Int, val active: Set<CellCoord>) {
    fun toMaskRows(): List<String> = (0 until rows).map { row ->
        (0 until cols).joinToString("") { col ->
            if (CellCoord(row, col) in active) ARROW_ESCAPE_MASK_ACTIVE.toString() else ARROW_ESCAPE_MASK_VOID.toString()
        }
    }
}

private const val SUPERSAMPLE = 4
private const val MAX_FILLED_PINHOLE = 2
private const val MIN_ISLAND_CELLS = 4
private const val MIN_ISLAND_SHARE = 0.02

private val silhouetteJson = Json { ignoreUnknownKeys = true }

fun parseArrowEscapeSilhouettes(text: String): List<ArrowEscapeSilhouette> =
    silhouetteJson.decodeFromString<List<ArrowEscapeSilhouette>>(text)

fun rasterizeSilhouette(silhouette: ArrowEscapeSilhouette, rows: Int): ArrowEscapeBoardMask {
    val cols = silhouette.colsForRows(rows)
    val active = mutableSetOf<CellCoord>()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            var hits = 0
            for (sy in 0 until SUPERSAMPLE) {
                for (sx in 0 until SUPERSAMPLE) {
                    val x = (col + (sx + 0.5) / SUPERSAMPLE) / cols * silhouette.width
                    val y = (row + (sy + 0.5) / SUPERSAMPLE) / rows * silhouette.height
                    if (silhouette.covers(x, y)) hits++
                }
            }
            if (2 * hits > SUPERSAMPLE * SUPERSAMPLE) active.add(CellCoord(row, col))
        }
    }

    val void = buildSet {
        for (row in 0 until rows) for (col in 0 until cols) {
            val cell = CellCoord(row, col)
            if (cell !in active) add(cell)
        }
    }
    for (component in connectedComponents(void)) {
        val touchesEdge = component.any { it.row == 0 || it.row == rows - 1 || it.col == 0 || it.col == cols - 1 }
        if (!touchesEdge && component.size <= MAX_FILLED_PINHOLE) active.addAll(component)
    }

    val minIsland = max(MIN_ISLAND_CELLS, ceil(active.size * MIN_ISLAND_SHARE).toInt())
    while (true) {
        val isolated = active.filter { cell -> neighbors(cell).none { it in active } }
        val islands = connectedComponents(active).filter { it.size < minIsland }.flatten()
        if (isolated.isEmpty() && islands.isEmpty()) break
        active.removeAll(isolated.toSet())
        active.removeAll(islands.toSet())
    }
    return ArrowEscapeBoardMask(rows, cols, active)
}

fun silhouetteBoardForCellTarget(
    silhouette: ArrowEscapeSilhouette,
    targetActiveCells: Int,
    maxRows: Int = 60,
    maxCols: Int = 45,
): ArrowEscapeBoardMask {
    var rows = silhouette.minRows
    while (true) {
        val mask = rasterizeSilhouette(silhouette, rows)
        val nextTooBig = rows + 1 > maxRows || silhouette.colsForRows(rows + 1) > maxCols
        if (mask.active.size >= targetActiveCells || nextTooBig) return mask
        rows++
    }
}

internal fun neighbors(cell: CellCoord): List<CellCoord> = listOf(
    CellCoord(cell.row - 1, cell.col),
    CellCoord(cell.row + 1, cell.col),
    CellCoord(cell.row, cell.col - 1),
    CellCoord(cell.row, cell.col + 1),
)

internal fun connectedComponents(cells: Set<CellCoord>): List<List<CellCoord>> {
    val seen = mutableSetOf<CellCoord>()
    val components = mutableListOf<List<CellCoord>>()
    for (start in cells) {
        if (!seen.add(start)) continue
        val stack = ArrayDeque(listOf(start))
        val component = mutableListOf<CellCoord>()
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            component.add(cell)
            for (next in neighbors(cell)) {
                if (next in cells && seen.add(next)) stack.addLast(next)
            }
        }
        components.add(component)
    }
    return components
}
