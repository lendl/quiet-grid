package com.quietgrid.app.games.links

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.isLinksPairConnected

private const val PIPE_WIDTH = 0.34f
private const val DOT_RADIUS = 0.34f
private const val RING_RADIUS = 0.44f
private const val RING_WIDTH = 0.06f
private const val FILL_ALPHA_CONNECTED = 0.22f
private const val FILL_ALPHA_OPEN = 0.12f
private const val HEAD_GLOW_RADIUS = 0.5f
private const val HEAD_GLOW_ALPHA = 0.3f

@Composable
fun LinksGrid(
    puzzle: LinksPuzzleEntry,
    paths: List<List<Int>>,
    activeColor: Int?,
    onDragStart: (Int) -> Unit,
    onDragMove: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val pencil = LocalIsPencilTheme.current
    val textMeasurer = rememberTextMeasurer()
    val currentPaths by rememberUpdatedState(paths)
    val pairColors = rememberLinksPairColors(puzzle)
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridLine = MaterialTheme.colorScheme.outlineVariant
    val letterFamily = MaterialTheme.typography.titleMedium.fontFamily

    BoxWithConstraints(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / puzzle.size, maxHeight / puzzle.size)
        val density = LocalDensity.current
        val cellPx = with(density) { cellSize.toPx() }
        val letterStyle = TextStyle(
            fontSize = with(density) { (cellPx * 0.36f).toSp() },
            fontWeight = FontWeight.Bold,
            fontFamily = letterFamily,
        )

        fun cellAt(position: Offset): Int? {
            val col = (position.x / cellPx).toInt()
            val row = (position.y / cellPx).toInt()
            if (position.x < 0f || position.y < 0f || row >= puzzle.size || col >= puzzle.size) return null
            return row * puzzle.size + col
        }

        Box(
            Modifier
                .size(cellSize * puzzle.size, cellSize * puzzle.size)
                .pointerInput(puzzle, cellPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val start = cellAt(down.position) ?: return@awaitEachGesture
                        down.consume()
                        onDragStart(start)
                        var last = start
                        val completed = drag(down.id) { change ->
                            change.consume()
                            val cell = cellAt(change.position) ?: return@drag
                            if (cell != last) {
                                last = cell
                                onDragMove(cell)
                            }
                        }
                        if (completed) onDragEnd() else onDragCancel()
                    }
                }
                .drawBehind {
                    val n = puzzle.size
                    drawRoundRect(background, cornerRadius = CornerRadius(cellPx * 0.2f))
                    fun center(cell: Int) = Offset((cell % n + 0.5f) * cellPx, (cell / n + 0.5f) * cellPx)

                    currentPaths.forEachIndexed { color, path ->
                        if (path.isEmpty()) return@forEachIndexed
                        val alpha = if (isLinksPairConnected(puzzle, currentPaths, color)) FILL_ALPHA_CONNECTED else FILL_ALPHA_OPEN
                        val tint = pairColors[color].copy(alpha = alpha)
                        path.forEach { cell ->
                            drawRect(tint, topLeft = Offset((cell % n) * cellPx, (cell / n) * cellPx), size = Size(cellPx, cellPx))
                        }
                    }

                    val lineWidth = maxOf(1f, cellPx * 0.02f)
                    for (k in 1 until n) {
                        drawLine(gridLine, Offset(k * cellPx, 0f), Offset(k * cellPx, n * cellPx), lineWidth)
                        drawLine(gridLine, Offset(0f, k * cellPx), Offset(n * cellPx, k * cellPx), lineWidth)
                    }

                    currentPaths.forEachIndexed { color, path ->
                        if (path.size < 2) return@forEachIndexed
                        val pipe = Path().apply {
                            val first = center(path.first())
                            moveTo(first.x, first.y)
                            path.drop(1).forEach { cell -> center(cell).let { lineTo(it.x, it.y) } }
                        }
                        drawPath(
                            pipe,
                            pairColors[color],
                            style = Stroke(width = cellPx * PIPE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }

                    puzzle.endpoints.forEachIndexed { color, ends ->
                        val pairColor = pairColors[color]
                        val connected = isLinksPairConnected(puzzle, currentPaths, color)
                        ends.forEach { cell ->
                            val c = center(cell)
                            if (connected) drawCircle(pairColor, radius = cellPx * RING_RADIUS, center = c, style = Stroke(cellPx * RING_WIDTH))
                            drawCircle(pairColor, radius = cellPx * DOT_RADIUS, center = c)
                            if (pencil) {
                                val layout = textMeasurer.measure(linksPairLetter(color), letterStyle.copy(color = linksLetterColor(pairColor)))
                                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
                            }
                        }
                    }

                    val active = activeColor?.let { currentPaths.getOrNull(it) }?.lastOrNull()
                    if (activeColor != null && active != null) {
                        drawCircle(
                            pairColors[activeColor].copy(alpha = HEAD_GLOW_ALPHA),
                            radius = cellPx * HEAD_GLOW_RADIUS,
                            center = center(active),
                        )
                    }
                },
        )
    }
}
