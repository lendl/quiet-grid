package com.quietgrid.app.games.hashi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.engine.hashi.HashiBoard

private val TAP_TOLERANCE = 18.dp
private val MIN_BRIDGE_STROKE = 1.5.dp

@Composable
fun HashiGrid(
    board: HashiBoard,
    bridges: List<Int>,
    islandStates: List<HashiIslandState>,
    highlight: HashiHighlight?,
    lastTap: HashiTapEvent?,
    onEdgeTap: (Int) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentBridges by rememberUpdatedState(bridges)
    val blockedShake = remember(board) { Animatable(0f) }
    LaunchedEffect(lastTap) {
        val tap = lastTap ?: return@LaunchedEffect
        if (!tap.blocked) return@LaunchedEffect
        blockedShake.snapTo(0f)
        blockedShake.animateTo(
            0f,
            animationSpec = keyframes {
                durationMillis = 300
                1f at 60
                -1f at 140
                0.6f at 210
                0f at 300
            },
        )
    }
    val dotColor = MaterialTheme.colorScheme.outlineVariant
    val bridgeColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.tertiary
    val blockedColor = MaterialTheme.colorScheme.outline
    val openFill = MaterialTheme.colorScheme.surface
    val openRing = MaterialTheme.colorScheme.primary
    val openText = MaterialTheme.colorScheme.onSurface
    val completeFill = MaterialTheme.colorScheme.primary
    val completeText = MaterialTheme.colorScheme.onPrimary
    val overFill = MaterialTheme.colorScheme.surfaceVariant
    val overRing = MaterialTheme.colorScheme.outline
    val overText = MaterialTheme.colorScheme.onSurfaceVariant
    val numberStyle = MaterialTheme.typography.titleMedium

    BoxWithConstraints(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / board.cols, maxHeight / board.rows)
        val density = LocalDensity.current
        val cellSizePx = with(density) { cellSize.toPx() }
        val fontSize = with(density) { (cellSizePx * 0.46f).toSp() }
        val textStyle = TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold, fontFamily = numberStyle.fontFamily)
        val blockedEdge = lastTap?.takeIf { it.blocked }?.edge

        Box(
            Modifier
                .size(cellSize * board.cols, cellSize * board.rows)
                .pointerInput(board, cellSizePx) {
                    val toleranceCells = TAP_TOLERANCE.toPx() / cellSizePx
                    detectTapGestures { tap ->
                        val edge = resolveHashiTap(
                            board,
                            tapRow = tap.y / cellSizePx,
                            tapCol = tap.x / cellSizePx,
                            toleranceCells = minOf(toleranceCells, 0.5f),
                        ) ?: return@detectTapGestures
                        onEdgeTap(edge)
                    }
                }
                .drawWithContent {
                    drawContent()
                    fun center(island: Int): Offset {
                        val spot = board.islands[island]
                        return Offset(cellSizePx * (spot.col + 0.5f), cellSizePx * (spot.row + 0.5f))
                    }

                    val dotRadius = cellSizePx * 0.04f
                    for (row in 0 until board.rows) for (col in 0 until board.cols) {
                        drawCircle(dotColor, radius = dotRadius, center = Offset(cellSizePx * (col + 0.5f), cellSizePx * (row + 0.5f)))
                    }

                    val stroke = maxOf(cellSizePx * 0.07f, MIN_BRIDGE_STROKE.toPx())
                    val gap = cellSizePx * 0.13f
                    val mistakeDash = PathEffect.dashPathEffect(floatArrayOf(cellSizePx * 0.12f, cellSizePx * 0.2f))
                    board.edges.forEachIndexed { index, edge ->
                        val count = currentBridges[index]
                        val hinted = highlight?.edge == index
                        val shake = if (index == blockedEdge) blockedShake.value * cellSizePx * 0.08f else 0f
                        if (count == 0 && !hinted && shake == 0f) return@forEachIndexed
                        val start = center(edge.a)
                        val end = center(edge.b)
                        val normal = if (edge.horizontal) Offset(0f, 1f) else Offset(1f, 0f)
                        when {
                            hinted && highlight.isMistake -> drawBridge(start, end, normal, count, gap, stroke, hintColor, mistakeDash)
                            hinted -> drawBridge(start, end, normal, highlight.count, gap, stroke, hintColor)
                            else -> drawBridge(start, end, normal, count, gap, stroke, bridgeColor)
                        }
                        if (shake != 0f) drawBridge(start + normal * shake, end + normal * shake, normal, 1, gap, stroke, blockedColor)
                    }

                    val radius = cellSizePx * 0.42f
                    val ring = maxOf(cellSizePx * 0.06f, MIN_BRIDGE_STROKE.toPx())
                    board.islands.forEachIndexed { index, island ->
                        val c = center(index)
                        val state = islandStates[index]
                        val fill = when (state) {
                            HashiIslandState.OPEN -> openFill
                            HashiIslandState.COMPLETE -> completeFill
                            HashiIslandState.OVERFULL -> overFill
                        }
                        val textColor = when (state) {
                            HashiIslandState.OPEN -> openText
                            HashiIslandState.COMPLETE -> completeText
                            HashiIslandState.OVERFULL -> overText
                        }
                        drawCircle(fill, radius = radius, center = c)
                        drawCircle(if (state == HashiIslandState.OVERFULL) overRing else openRing, radius = radius - ring / 2, center = c, style = Stroke(ring))
                        val layout = textMeasurer.measure(island.count.toString(), textStyle.copy(color = textColor))
                        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
                    }
                },
        )
    }
}

private fun DrawScope.drawBridge(
    start: Offset,
    end: Offset,
    normal: Offset,
    count: Int,
    gap: Float,
    stroke: Float,
    color: Color,
    pathEffect: PathEffect? = null,
) {
    when (count) {
        1 -> drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round, pathEffect = pathEffect)
        2 -> {
            drawLine(color, start + normal * gap, end + normal * gap, strokeWidth = stroke, cap = StrokeCap.Round, pathEffect = pathEffect)
            drawLine(color, start - normal * gap, end - normal * gap, strokeWidth = stroke, cap = StrokeCap.Round, pathEffect = pathEffect)
        }
    }
}
