package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.quietgrid.app.games.animaldoku.rememberAnimalDokuRegionColors
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.arrowescape.ARROW_ESCAPE_MASK_ACTIVE
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData

sealed interface JourneyBoardPreviewData {
    data class Arrows(val rows: Int, val cols: Int, val pieces: List<ArrowEscapePieceData>, val mask: List<String>? = null) : JourneyBoardPreviewData

    data class Regions(val size: Int, val regions: List<List<Int>>) : JourneyBoardPreviewData
}

@Composable
fun JourneyBoardPreview(data: JourneyBoardPreviewData, modifier: Modifier = Modifier) {
    when (data) {
        is JourneyBoardPreviewData.Arrows -> ArrowsPreview(data, modifier)
        is JourneyBoardPreviewData.Regions -> RegionsPreview(data, modifier)
    }
}

@Composable
private fun ArrowsPreview(data: JourneyBoardPreviewData.Arrows, modifier: Modifier) {
    val dot = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0) return@Canvas
        val cell = minOf(size.width / data.cols, size.height / data.rows)
        val left = (size.width - cell * data.cols) / 2
        val top = (size.height - cell * data.rows) / 2
        fun centre(row: Int, col: Int) = Offset(left + (col + 0.5f) * cell, top + (row + 0.5f) * cell)
        for (row in 0 until data.rows) {
            for (col in 0 until data.cols) {
                val active = data.mask?.getOrNull(row)?.getOrNull(col)?.let { it == ARROW_ESCAPE_MASK_ACTIVE } ?: true
                if (active) drawCircle(dot, radius = cell * 0.12f, center = centre(row, col))
            }
        }
        val stroke = Stroke(width = cell * 0.32f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        data.pieces.forEach { piece ->
            val points = piece.cells.map { centre(it[0], it[1]) }
            if (points.isEmpty()) return@forEach
            val (dx, dy) = when (piece.headDirection) {
                "up" -> 0f to -1f
                "down" -> 0f to 1f
                "left" -> -1f to 0f
                else -> 1f to 0f
            }
            val head = points.last()
            val tip = Offset(head.x + dx * cell * 0.4f, head.y + dy * cell * 0.4f)
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(tip.x, tip.y)
                val back = cell * 0.3f
                moveTo(tip.x - dx * back - dy * back, tip.y - dy * back + dx * back)
                lineTo(tip.x, tip.y)
                lineTo(tip.x - dx * back + dy * back, tip.y - dy * back - dx * back)
            }
            drawPath(path, ink, style = stroke)
        }
    }
}

@Composable
private fun RegionsPreview(data: JourneyBoardPreviewData.Regions, modifier: Modifier) {
    val pencil = LocalIsPencilTheme.current
    val regionColors = rememberAnimalDokuRegionColors(data.regions)
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.size <= 0) return@Canvas
        val cell = minOf(size.width, size.height) / data.size
        val left = (size.width - cell * data.size) / 2
        val top = (size.height - cell * data.size) / 2
        for (row in 0 until data.size) {
            for (col in 0 until data.size) {
                val region = data.regions.getOrNull(row)?.getOrNull(col) ?: continue
                val colour = if (pencil) surface else regionColors.getOrElse(region) { surface }
                drawRect(colour, Offset(left + col * cell, top + row * cell), Size(cell, cell))
            }
        }
        val thin = cell * 0.04f
        val thick = cell * 0.12f
        for (row in 0 until data.size) {
            for (col in 0 until data.size) {
                val region = data.regions.getOrNull(row)?.getOrNull(col) ?: continue
                val x = left + col * cell
                val y = top + row * cell
                if (col + 1 < data.size) {
                    val split = data.regions[row][col + 1] != region
                    drawLine(if (split && pencil) line else grid, Offset(x + cell, y), Offset(x + cell, y + cell), if (split && pencil) thick else thin)
                }
                if (row + 1 < data.size) {
                    val split = data.regions[row + 1][col] != region
                    drawLine(if (split && pencil) line else grid, Offset(x, y + cell), Offset(x + cell, y + cell), if (split && pencil) thick else thin)
                }
            }
        }
    }
}
