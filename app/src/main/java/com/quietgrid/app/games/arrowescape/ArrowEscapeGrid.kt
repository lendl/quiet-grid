package com.quietgrid.app.games.arrowescape

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.arrowescape.ArrowDirection
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.isActiveCell
import com.quietgrid.engine.arrowescape.toPiece
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.launch

private val TAP_TOLERANCE = 12.dp
private val MIN_DOT_RADIUS = 1.dp
private val MIN_LINE_STROKE = 2.dp
private const val ARC_HANDLE = 0.5523f

private val EscapeEasing = CubicBezierEasing(0.45f, 0f, 0.8f, 0.4f)
private val BumpForwardEasing = CubicBezierEasing(0.5f, 0f, 1f, 1f)
private val BackOutEasing = Easing { t ->
    val u = t - 1f
    1f + 2.2f * u * u * u + 1.2f * u * u
}

private class EscapeAnimation(
    val progress: Animatable<Float, AnimationVector1D>,
    val distCells: Int,
    val durationMs: Int,
)

private class BumpAnimation(
    val progress: Animatable<Float, AnimationVector1D>,
    val tint: Animatable<Float, AnimationVector1D>,
    val blockerOffset: Int,
)

private class KnockAnimation(
    val elapsedMs: Animatable<Float, AnimationVector1D>,
    val direction: Offset,
)

private class ArrowBlocker(val offset: Int, val pieceIndex: Int)

private fun findBlocker(
    pieceIndex: Int,
    piece: ArrowEscapePiece,
    ownerLookup: Map<Pair<Int, Int>, Int>,
    removedIndices: Set<Int>,
    rows: Int,
    cols: Int,
): ArrowBlocker? {
    val head = piece.cells.last()
    val corridor = computeCorridor(head.row, head.col, piece.headDirection, rows, cols)
    corridor.forEachIndexed { offset, cell ->
        val owner = ownerLookup[cell.row to cell.col]
        if (owner != null && owner != pieceIndex && owner !in removedIndices) return ArrowBlocker(offset, owner)
    }
    return null
}

private fun cellCenter(cell: CellCoord, cellSizePx: Float): Offset =
    Offset(cellSizePx * (cell.col + 0.5f), cellSizePx * (cell.row + 0.5f))

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
        val inbound = toCurr / distToCurr
        val outbound = toNext / distToNext
        val before = curr - inbound * radius
        val after = curr + outbound * radius
        val control1 = before + inbound * (radius * ARC_HANDLE)
        val control2 = after - outbound * (radius * ARC_HANDLE)
        lineTo(before.x, before.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, after.x, after.y)
    }
    lineTo(points.last().x, points.last().y)
}

private fun shaftPath(points: List<Offset>, end: Offset, cornerRadius: Float): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    roundedPolylineTo(points + end, cornerRadius)
}

private fun headPath(tip: Offset, direction: Offset, headLength: Float, headHalfWidth: Float, cornerInset: Float): Path {
    val base = tip - direction * headLength
    val perp = Offset(-direction.y, direction.x)
    val apex = tip - direction * cornerInset
    val wing1 = base + perp * (headHalfWidth - cornerInset)
    val wing2 = base - perp * (headHalfWidth - cornerInset)
    return Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(wing1.x, wing1.y)
        lineTo(wing2.x, wing2.y)
        close()
    }
}

@Composable
fun ArrowEscapeGrid(
    puzzle: ArrowEscapePuzzleEntry,
    removedIndices: Set<Int>,
    selectedIndex: Int?,
    lastTap: ArrowEscapeTapEvent?,
    visibleBounds: Rect?,
    onRequestPan: (Offset?) -> Unit,
    onPieceTap: (Int) -> Unit,
) {
    val isPencilTheme = LocalIsPencilTheme.current
    val pieces = remember(puzzle) { puzzle.pieces.map { it.toPiece() } }
    val ownerLookup = remember(puzzle) { buildCellOwnerMap(pieces) }
    val viewport = remember(puzzle) { arrowEscapeViewport(puzzle) }
    val corridorSizes = remember(puzzle) {
        pieces.map { piece ->
            val head = piece.cells.last()
            computeCorridor(head.row, head.col, piece.headDirection, puzzle.rows, puzzle.cols).size
        }
    }
    val flyOuts = remember(puzzle) { mutableStateMapOf<Int, EscapeAnimation>() }
    val laneGlows = remember(puzzle) { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    val bumps = remember(puzzle) { mutableStateMapOf<Int, BumpAnimation>() }
    val knocks = remember(puzzle) { mutableStateMapOf<Int, KnockAnimation>() }
    val animationScope = rememberCoroutineScope()
    var previousRemoved by remember(puzzle) { mutableStateOf(removedIndices) }
    val currentRemoved by rememberUpdatedState(removedIndices)
    val blockedIndex = lastTap?.takeIf { !it.removed }?.pieceIndex
    val hintedIndex = selectedIndex?.takeIf { it != blockedIndex && it !in removedIndices }

    LaunchedEffect(removedIndices) {
        val newlyRemoved = removedIndices - previousRemoved
        previousRemoved = removedIndices
        for (index in newlyRemoved) {
            val distCells = (pieces[index].cells.size - 1) + corridorSizes[index] + 2
            val escape = EscapeAnimation(Animatable(0f), distCells, minOf(360 + 18 * distCells, 900))
            val glow = Animatable(0f)
            flyOuts[index] = escape
            laneGlows[index] = glow
            animationScope.launch {
                escape.progress.animateTo(1f, animationSpec = tween(escape.durationMs, easing = LinearEasing))
                if (flyOuts[index] === escape) flyOuts.remove(index)
            }
            animationScope.launch {
                glow.animateTo(1f, animationSpec = tween(380, easing = LinearEasing))
                if (laneGlows[index] === glow) laneGlows.remove(index)
            }
        }
    }

    LaunchedEffect(lastTap) {
        val tap = lastTap ?: return@LaunchedEffect
        if (tap.removed) return@LaunchedEffect
        val index = tap.pieceIndex
        val piece = pieces[index]
        val blocker = findBlocker(index, piece, ownerLookup, currentRemoved, puzzle.rows, puzzle.cols)
        val bump = BumpAnimation(Animatable(0f), Animatable(0f), blocker?.offset ?: corridorSizes[index])
        bumps[index] = bump
        animationScope.launch {
            val tintJob = launch {
                bump.tint.animateTo(1f, animationSpec = tween(200, easing = LinearEasing))
                bump.tint.animateTo(0f, animationSpec = tween(350, easing = LinearEasing))
            }
            bump.progress.animateTo(1f, animationSpec = tween(200, easing = BumpForwardEasing))
            if (blocker != null && bumps[index] === bump) {
                val blockerIndex = blocker.pieceIndex
                val knock = KnockAnimation(Animatable(0f), directionVector(piece.headDirection))
                knocks[blockerIndex] = knock
                launch {
                    knock.elapsedMs.animateTo(600f, animationSpec = tween(600, easing = LinearEasing))
                    if (knocks[blockerIndex] === knock) knocks.remove(blockerIndex)
                }
            }
            bump.progress.animateTo(0f, animationSpec = tween(420, easing = BackOutEasing))
            tintJob.join()
            if (bumps[index] === bump) bumps.remove(index)
        }
    }

    BoxWithConstraints(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / viewport.cols, maxHeight / viewport.rows)
        val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }
        val viewportOrigin = Offset(cellSizePx * viewport.left, cellSizePx * viewport.top)

        LaunchedEffect(removedIndices, visibleBounds, cellSizePx, puzzle) {
            val remaining = pieces.indices.filter { it !in removedIndices }
            val bounds = visibleBounds
            if (remaining.isEmpty() || bounds == null) {
                onRequestPan(null)
                return@LaunchedEffect
            }
            val anyVisible = remaining.any { index ->
                pieces[index].cells.any { cell ->
                    bounds.contains(cellCenter(cell, cellSizePx) - viewportOrigin)
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
                    val center = cellCenter(cell, cellSizePx) - viewportOrigin
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
            onRequestPan(Offset(targetX, targetY) - viewportOrigin)
        }

        val dotColor = if (isPencilTheme) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
        val lineColor = if (isPencilTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        val escapeColor = MaterialTheme.colorScheme.primary
        val laneColor = if (isPencilTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
        val errorColor = MaterialTheme.colorScheme.error
        val hintColor = MaterialTheme.colorScheme.primary

        Box(
            Modifier
                .size(cellSize * viewport.cols, cellSize * viewport.rows)
                .pointerInput(puzzle, cellSizePx, viewport) {
                    val toleranceCells = TAP_TOLERANCE.toPx() / cellSizePx
                    detectTapGestures { tapOffset ->
                        val pieceIndex = resolveArrowEscapeTap(
                            puzzle,
                            currentRemoved,
                            tapRow = tapOffset.y / cellSizePx + viewport.top,
                            tapCol = tapOffset.x / cellSizePx + viewport.left,
                            toleranceCells = toleranceCells,
                        ) ?: return@detectTapGestures
                        onPieceTap(pieceIndex)
                    }
                }
                .drawWithContent {
                    drawContent()
                    translate(left = -viewportOrigin.x, top = -viewportOrigin.y) {
                        val dotRadius = maxOf(cellSizePx * 0.05f, MIN_DOT_RADIUS.toPx())
                        for (row in 0 until puzzle.rows) {
                            for (col in 0 until puzzle.cols) {
                                if (!puzzle.isActiveCell(row, col)) continue
                                drawCircle(
                                    dotColor,
                                    radius = dotRadius,
                                    center = Offset(cellSizePx * (col + 0.5f), cellSizePx * (row + 0.5f)),
                                )
                            }
                        }
                        val lineWidth = maxOf(cellSizePx * 0.115f, MIN_LINE_STROKE.toPx())
                        val headLength = cellSizePx * 0.46f
                        val headHalfWidth = maxOf(cellSizePx * 0.27f, lineWidth * 1.6f)
                        val headCornerInset = lineWidth * 0.35f
                        val cornerRadius = cellSizePx * 0.3f
                        val lineStroke = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val headOutline = Stroke(width = headCornerInset * 2f, join = StrokeJoin.Round)

                        laneGlows.forEach { (index, glow) ->
                            val piece = pieces[index]
                            val direction = directionVector(piece.headDirection)
                            val start = cellCenter(piece.cells.last(), cellSizePx)
                            val end = start + direction * (cellSizePx * (corridorSizes[index] + 0.5f))
                            drawLine(
                                laneColor.copy(alpha = 0.3f * (1f - glow.value)),
                                start = start,
                                end = end,
                                strokeWidth = lineWidth * 2.4f,
                                cap = StrokeCap.Round,
                            )
                        }

                        pieces.forEachIndexed { index, piece ->
                            val escape = flyOuts[index]
                            if (index in removedIndices && escape == null) return@forEachIndexed
                            val direction = directionVector(piece.headDirection)
                            val hinted = index == hintedIndex && escape == null
                            var color = if (hinted) hintColor else lineColor
                            var alpha = 1f
                            var travelled = 0f
                            var shift = Offset.Zero

                            if (escape != null) {
                                val t = escape.progress.value
                                travelled = cellSizePx * escape.distCells * EscapeEasing.transform(t)
                                if (!isPencilTheme) {
                                    color = lerp(lineColor, escapeColor, (t * escape.durationMs / 120f).coerceIn(0f, 1f))
                                }
                                if (t >= 0.8f) alpha = 1f - (t - 0.8f) / 0.2f
                            }

                            val bump = bumps[index]
                            if (bump != null) {
                                travelled = cellSizePx * (bump.blockerOffset + 0.45f) * bump.progress.value
                                color = lerp(lineColor, errorColor, bump.tint.value)
                            }

                            val knock = knocks[index]
                            if (knock != null) {
                                val ms = knock.elapsedMs.value
                                val wobble = cellSizePx * 0.12f * exp(-ms / 140f) * sin(ms / 600f * 5f * PI.toFloat() + 0.3f)
                                shift = knock.direction * wobble
                                color = lerp(lineColor, errorColor, (1f - ms / 450f).coerceAtLeast(0f))
                            }

                            val basePoints = piece.cells.map { cellCenter(it, cellSizePx) + shift }
                            val stubEnd = basePoints.last() + direction * (cellSizePx * (piece.cells.size + corridorSizes[index] + 3))
                            val extendedPoints = basePoints + stubEnd
                            val cum = cumulativeArcLengths(extendedPoints)
                            val pieceLength = cum[basePoints.lastIndex]

                            val tailArc = travelled.coerceAtLeast(0f)
                            val headArc = pieceLength + travelled

                            val visiblePoints = buildList {
                                add(pointAtArc(extendedPoints, cum, tailArc))
                                for (i in extendedPoints.indices) {
                                    if (cum[i] > tailArc && cum[i] < headArc) add(extendedPoints[i])
                                }
                                add(pointAtArc(extendedPoints, cum, headArc))
                            }

                            val pieceColor = color.copy(alpha = color.alpha * alpha)
                            val tip = visiblePoints.last() + direction * (cellSizePx * 0.42f)
                            val shaftEnd = tip - direction * (headLength - lineWidth * 0.5f)
                            val head = headPath(tip, direction, headLength, headHalfWidth, headCornerInset)
                            val shaft = shaftPath(visiblePoints, shaftEnd, cornerRadius)
                            if (hinted) {
                                val halo = Stroke(width = lineWidth * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                drawPath(shaft, hintColor.copy(alpha = 0.22f), style = halo)
                                drawPath(head, hintColor.copy(alpha = 0.22f), style = halo)
                            }
                            drawPath(shaft, pieceColor, style = lineStroke)
                            drawPath(head, pieceColor)
                            drawPath(head, pieceColor, style = headOutline)
                        }
                    }
                },
        )
    }
}
