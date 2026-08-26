package com.quietgrid.app.games.arrowescape

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.toPiece
import kotlin.math.floor
import kotlinx.coroutines.launch

private fun offsetToCell(offset: Offset, cellSizePx: Float, rows: Int, cols: Int): Pair<Int, Int>? {
    val col = floor(offset.x / cellSizePx).toInt()
    val row = floor(offset.y / cellSizePx).toInt()
    if (row !in 0 until rows || col !in 0 until cols) return null
    return row to col
}

private fun directionVector(direction: ArrowDirection): Offset = when (direction) {
    ArrowDirection.UP -> Offset(0f, -1f)
    ArrowDirection.DOWN -> Offset(0f, 1f)
    ArrowDirection.LEFT -> Offset(-1f, 0f)
    ArrowDirection.RIGHT -> Offset(1f, 0f)
}

private fun cumulativeArcLengths(points: List<Offset>): List<Float> {
    val cum = mutableListOf(0f)
    for (i in 1 until points.size) cum.add(cum.last() + (points[i] - points[i - 1]).getDistance())
    return cum
}

private fun pointAtArc(points: List<Offset>, cum: List<Float>, arc: Float): Offset {
    val clamped = arc.coerceIn(0f, cum.last())
    for (i in 1 until points.size) {
        if (clamped <= cum[i]) {
            val segLength = cum[i] - cum[i - 1]
            val t = if (segLength > 0f) (clamped - cum[i - 1]) / segLength else 0f
            return Offset(
                points[i - 1].x + (points[i].x - points[i - 1].x) * t,
                points[i - 1].y + (points[i].y - points[i - 1].y) * t,
            )
        }
    }
    return points.last()
}

private fun Path.roundedPolylineTo(points: List<Offset>, cornerRadius: Float) {
    if (points.size < 2) return
    if (points.size == 2) {
        lineTo(points[1].x, points[1].y)
        return
    }
    for (i in 1 until points.size - 1) {
        val prev = points[i - 1]
        val curr = points[i]
        val next = points[i + 1]
        val toCurr = curr - prev
        val toNext = next - curr
        val distToCurr = toCurr.getDistance()
        val distToNext = toNext.getDistance()
        val radius = minOf(cornerRadius, distToCurr / 2f, distToNext / 2f)
        if (radius <= 0.01f) {
            lineTo(curr.x, curr.y)
            continue
        }
        val before = curr - toCurr / distToCurr * radius
        val after = curr + toNext / distToNext * radius
        lineTo(before.x, before.y)
        quadraticBezierTo(curr.x, curr.y, after.x, after.y)
    }
    lineTo(points.last().x, points.last().y)
}

private fun piecePath(
    cellCenters: List<Offset>,
    tip: Offset,
    direction: Offset,
    headLength: Float,
    headHalfWidth: Float,
    cornerRadius: Float,
): Path {
    val back = Offset(tip.x - direction.x * headLength, tip.y - direction.y * headLength)
    val perp = Offset(-direction.y, direction.x)
    val wing1 = Offset(back.x + perp.x * headHalfWidth, back.y + perp.y * headHalfWidth)
    val wing2 = Offset(back.x - perp.x * headHalfWidth, back.y - perp.y * headHalfWidth)
    return Path().apply {
        moveTo(cellCenters.first().x, cellCenters.first().y)
        roundedPolylineTo(cellCenters + back, cornerRadius)
        moveTo(wing1.x, wing1.y)
        lineTo(tip.x, tip.y)
        lineTo(wing2.x, wing2.y)
    }
}

@Composable
fun ArrowEscapeGrid(
    puzzle: ArrowEscapePuzzleEntry,
    removedIndices: Set<Int>,
    selectedIndex: Int?,
    blockedIndex: Int?,
    visibleBounds: Rect?,
    onRequestPan: (Offset?) -> Unit,
    onPieceTap: (Int) -> Unit,
) {
    val isPencilTheme = LocalIsPencilTheme.current
    val pieces = remember(puzzle) { puzzle.pieces.map { it.toPiece() } }
    val ownerLookup = remember(puzzle) { buildCellOwnerMap(pieces) }
    val flyOuts = remember(puzzle) { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    val bumps = remember(puzzle) { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    val animationScope = rememberCoroutineScope()
    var previousRemoved by remember(puzzle) { mutableStateOf(removedIndices) }

    LaunchedEffect(removedIndices) {
        val newlyRemoved = removedIndices - previousRemoved
        previousRemoved = removedIndices
        for (index in newlyRemoved) {
            val progress = Animatable(0f)
            flyOuts[index] = progress
            animationScope.launch {
                progress.animateTo(1f, animationSpec = tween(1100))
                flyOuts.remove(index)
            }
        }
    }

    LaunchedEffect(blockedIndex) {
        val index = blockedIndex ?: return@LaunchedEffect
        val progress = Animatable(0f)
        bumps[index] = progress
        progress.animateTo(1f, animationSpec = tween(220))
        progress.animateTo(0f, animationSpec = tween(320))
        bumps.remove(index)
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / puzzle.cols, maxHeight / puzzle.rows)
        val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }

        LaunchedEffect(removedIndices, visibleBounds, cellSizePx, puzzle) {
            val remaining = pieces.indices.filter { it !in removedIndices }
            val bounds = visibleBounds
            if (remaining.isEmpty() || bounds == null) {
                onRequestPan(null)
                return@LaunchedEffect
            }
            val anyVisible = remaining.any { index ->
                pieces[index].cells.any { cell ->
                    bounds.contains(Offset(cellSizePx * (cell.col + 0.5f), cellSizePx * (cell.row + 0.5f)))
                }
            }
            if (anyVisible) {
                onRequestPan(null)
                return@LaunchedEffect
            }
            val viewportCenter = bounds.center
            var nearestIndex = remaining.first()
            var nearestDistance = Float.MAX_VALUE
            for (index in remaining) {
                for (cell in pieces[index].cells) {
                    val center = Offset(cellSizePx * (cell.col + 0.5f), cellSizePx * (cell.row + 0.5f))
                    val distance = (center - viewportCenter).getDistance()
                    if (distance < nearestDistance) {
                        nearestDistance = distance
                        nearestIndex = index
                    }
                }
            }
            val targetCells = pieces[nearestIndex].cells
            val targetX = targetCells.map { cellSizePx * (it.col + 0.5f) }.average().toFloat()
            val targetY = targetCells.map { cellSizePx * (it.row + 0.5f) }.average().toFloat()
            onRequestPan(Offset(targetX, targetY))
        }

        val dotColor = if (isPencilTheme) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
        val lineColor = if (isPencilTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        val selectedColor = MaterialTheme.colorScheme.primary

        Box(
            Modifier
                .size(cellSize * puzzle.cols, cellSize * puzzle.rows)
                .pointerInput(puzzle, cellSizePx) {
                    detectTapGestures { tapOffset ->
                        val cell = offsetToCell(tapOffset, cellSizePx, puzzle.rows, puzzle.cols) ?: return@detectTapGestures
                        val pieceIndex = ownerLookup[cell] ?: return@detectTapGestures
                        onPieceTap(pieceIndex)
                    }
                }
                .drawWithContent {
                    drawContent()
                    val dotRadius = cellSizePx * 0.05f
                    for (row in 0 until puzzle.rows) {
                        for (col in 0 until puzzle.cols) {
                            drawCircle(
                                dotColor,
                                radius = dotRadius,
                                center = Offset(cellSizePx * (col + 0.5f), cellSizePx * (row + 0.5f)),
                            )
                        }
                    }
                    val lineStrokeWidth = cellSizePx * 0.07f
                    val headLength = cellSizePx * 0.22f
                    val headHalfWidth = cellSizePx * 0.14f
                    val cornerRadius = cellSizePx * 0.35f
                    val lineStroke = Stroke(width = lineStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val stubLength = cellSizePx * (maxOf(puzzle.rows, puzzle.cols) + 4f)
                    pieces.forEachIndexed { index, piece ->
                        val flyOutProgress = flyOuts[index]?.value
                        if (index in removedIndices && flyOutProgress == null) return@forEachIndexed
                        val progress: Float = flyOutProgress ?: 0f
                        val direction = directionVector(piece.headDirection)
                        val bumpProgress = bumps[index]?.value ?: 0f
                        val bumpOffset = if (bumpProgress > 0f) {
                            val head = piece.cells.last()
                            val corridor = computeCorridor(head.row, head.col, piece.headDirection, puzzle.rows, puzzle.cols)
                            val blockerOffset = corridor.indexOfFirst { cell ->
                                val owner = ownerLookup[cell.row to cell.col]
                                owner != null && owner !in removedIndices
                            }
                            val travelCells = if (blockerOffset >= 0) blockerOffset else corridor.size
                            val bumpDistance = cellSizePx * travelCells * bumpProgress
                            Offset(direction.x * bumpDistance, direction.y * bumpDistance)
                        } else {
                            Offset.Zero
                        }

                        val basePoints = piece.cells.map {
                            Offset(cellSizePx * (it.col + 0.5f) + bumpOffset.x, cellSizePx * (it.row + 0.5f) + bumpOffset.y)
                        }
                        val stubEnd = basePoints.last() + direction * stubLength
                        val extendedPoints = basePoints + stubEnd
                        val cum = cumulativeArcLengths(extendedPoints)
                        val pieceLength = cum[basePoints.lastIndex]

                        val travelled = stubLength * progress
                        val tailArc = travelled
                        val headArc = pieceLength + travelled

                        val visiblePoints = buildList {
                            add(pointAtArc(extendedPoints, cum, tailArc))
                            for (i in extendedPoints.indices) {
                                if (cum[i] > tailArc && cum[i] < headArc) add(extendedPoints[i])
                            }
                            add(pointAtArc(extendedPoints, cum, headArc))
                        }

                        val pieceColor = if (progress > 0f) {
                            val head = piece.cells.last()
                            val onBoardStubCells = computeCorridor(head.row, head.col, piece.headDirection, puzzle.rows, puzzle.cols).size
                            val edgeArc = pieceLength + cellSizePx * onBoardStubCells
                            val fadeStartProgress = (edgeArc / stubLength).coerceIn(0f, 1f)
                            val alpha = if (progress <= fadeStartProgress) {
                                1f
                            } else {
                                1f - ((progress - fadeStartProgress) / (1f - fadeStartProgress)).coerceIn(0f, 1f)
                            }
                            lineColor.copy(alpha = alpha)
                        } else {
                            lineColor
                        }

                        val headPoint = visiblePoints.last()
                        val tip = Offset(headPoint.x + direction.x * cellSizePx * 0.4f, headPoint.y + direction.y * cellSizePx * 0.4f)
                        drawPath(piecePath(visiblePoints, tip, direction, headLength, headHalfWidth, cornerRadius), pieceColor, style = lineStroke)
                    }
                    if (selectedIndex != null && selectedIndex != blockedIndex && selectedIndex !in removedIndices) {
                        for (cell in pieces[selectedIndex].cells) {
                            val x = cellSizePx * cell.col
                            val y = cellSizePx * cell.row
                            drawRect(
                                selectedColor,
                                topLeft = Offset(x, y),
                                size = Size(cellSizePx, cellSizePx),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                },
        )
    }
}
