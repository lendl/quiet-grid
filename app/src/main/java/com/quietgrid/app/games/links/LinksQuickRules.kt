package com.quietgrid.app.games.links

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private const val MINI_COLS = 6
private const val MINI_ROWS = 4
private val MINI_CELL = 10.dp

private class MiniPipe(val color: Int, val cells: List<Pair<Int, Int>>, val endDot: Boolean = true)

@Composable
internal fun LinksConnectPairsPicture() {
    LinksMiniPicture(
        listOf(MiniPipe(0, listOf(0 to 1, 1 to 1, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 3 to 4))),
    )
}

@Composable
internal fun LinksNoCrossingPicture() {
    LinksMiniPicture(
        listOf(
            MiniPipe(0, (0 until MINI_COLS).map { 1 to it }),
            MiniPipe(1, listOf(0 to 2)),
            MiniPipe(1, listOf(3 to 2, 2 to 2), endDot = false),
        ),
    )
}

@Composable
internal fun LinksFillEveryCellPicture() {
    LinksMiniPicture(
        listOf(
            MiniPipe(0, (0 until MINI_COLS).map { 0 to it }),
            MiniPipe(1, (0 until MINI_COLS).map { 1 to it } + listOf(2 to 5, 3 to 5)),
            MiniPipe(2, (4 downTo 0).map { 2 to it } + (0..4).map { 3 to it }),
        ),
        tintCells = true,
    )
}

@Composable
private fun LinksMiniPicture(pipes: List<MiniPipe>, tintCells: Boolean = false) {
    val pencil = LocalIsPencilTheme.current
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridLine = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(MINI_CELL * MINI_COLS, MINI_CELL * MINI_ROWS)) {
        val cell = MINI_CELL.toPx()
        drawRoundRect(background, cornerRadius = CornerRadius(cell * 0.3f))
        fun center(spot: Pair<Int, Int>) = Offset((spot.second + 0.5f) * cell, (spot.first + 0.5f) * cell)
        if (tintCells) {
            pipes.forEach { pipe ->
                val tint = linksPaletteColor(pipe.color, pencil).copy(alpha = 0.22f)
                pipe.cells.forEach { drawRect(tint, Offset(it.second * cell, it.first * cell), Size(cell, cell)) }
            }
        }
        for (k in 1 until MINI_COLS) drawLine(gridLine, Offset(k * cell, 0f), Offset(k * cell, MINI_ROWS * cell), 1f)
        for (k in 1 until MINI_ROWS) drawLine(gridLine, Offset(0f, k * cell), Offset(MINI_COLS * cell, k * cell), 1f)
        pipes.forEach { pipe ->
            val color = linksPaletteColor(pipe.color, pencil)
            if (pipe.cells.size > 1) {
                val path = Path().apply {
                    center(pipe.cells.first()).let { moveTo(it.x, it.y) }
                    pipe.cells.drop(1).forEach { spot -> center(spot).let { lineTo(it.x, it.y) } }
                }
                drawPath(path, color, style = Stroke(cell * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawCircle(color, radius = cell * 0.36f, center = center(pipe.cells.first()))
            if (pipe.endDot && pipe.cells.size > 1) drawCircle(color, radius = cell * 0.36f, center = center(pipe.cells.last()))
        }
    }
}
