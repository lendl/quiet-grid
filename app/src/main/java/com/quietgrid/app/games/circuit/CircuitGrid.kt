package com.quietgrid.app.games.circuit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.components.rememberReduceMotion
import com.quietgrid.engine.circuit.CIRCUIT_DIRECTIONS
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP
import com.quietgrid.engine.circuit.CircuitBoard
import com.quietgrid.engine.circuit.circuitArmCount
import kotlinx.coroutines.launch

private const val ROTATE_MILLIS = 140

private fun armEnd(direction: Int, center: Offset, half: Float): Offset = when (direction) {
    CIRCUIT_UP -> Offset(center.x, center.y - half)
    CIRCUIT_DOWN -> Offset(center.x, center.y + half)
    CIRCUIT_LEFT -> Offset(center.x - half, center.y)
    CIRCUIT_RIGHT -> Offset(center.x + half, center.y)
    else -> center
}

internal fun DrawScope.drawCircuitTile(
    mask: Int,
    topLeft: Offset,
    cellSize: Float,
    lit: Boolean,
    isSource: Boolean,
    locked: Boolean,
    palette: CircuitPalette,
    angle: Float = 0f,
    hinted: Boolean = false,
) {
    val inset = cellSize * 0.04f
    val fill = when {
        hinted -> palette.hintTile
        locked -> palette.lockedTile
        else -> palette.tile
    }
    drawRoundRect(
        color = fill,
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(cellSize - inset * 2, cellSize - inset * 2),
        cornerRadius = CornerRadius(cellSize * 0.16f),
    )
    val center = Offset(topLeft.x + cellSize / 2, topLeft.y + cellSize / 2)
    val wireColor = if (lit) palette.litWire else palette.wire
    val stroke = cellSize * 0.15f
    rotate(angle, pivot = center) {
        for (direction in CIRCUIT_DIRECTIONS) {
            if (mask and direction == 0) continue
            drawLine(wireColor, center, armEnd(direction, center, cellSize / 2), strokeWidth = stroke, cap = StrokeCap.Butt)
        }
        drawCircle(wireColor, radius = stroke / 2, center = center)
    }
    when {
        isSource -> {
            val side = cellSize * 0.4f
            drawRoundRect(
                color = palette.litWire,
                topLeft = Offset(center.x - side / 2, center.y - side / 2),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * 0.25f),
            )
            drawCircle(palette.lampCore, radius = side * 0.18f, center = center)
        }
        circuitArmCount(mask) == 1 -> {
            val radius = cellSize * 0.2f
            if (lit) {
                drawCircle(palette.litWire, radius = radius, center = center)
                drawCircle(palette.lampCore, radius = radius * 0.4f, center = center)
            } else {
                drawCircle(palette.lampFill, radius = radius, center = center)
                drawCircle(palette.wire, radius = radius - stroke / 4, center = center, style = Stroke(stroke / 2))
            }
        }
    }
}

@Composable
fun CircuitGrid(
    board: CircuitBoard,
    startMasks: List<Int>,
    turns: List<Int>,
    locked: List<Boolean>,
    lit: BooleanArray,
    hintCell: Int?,
    lastTap: CircuitTapEvent?,
    onTileTap: (Int) -> Unit,
    onTileLongPress: (Int) -> Unit,
) {
    val palette = rememberCircuitPalette()
    val reduceMotion = rememberReduceMotion()
    val angles = remember(board) { List(board.size) { Animatable(turns[it] * 90f) } }
    LaunchedEffect(turns, reduceMotion) {
        turns.forEachIndexed { cell, turn ->
            val target = turn * 90f
            val angle = angles[cell]
            if (angle.value == target) return@forEachIndexed
            if (reduceMotion) launch { angle.snapTo(target) } else launch { angle.animateTo(target, tween(ROTATE_MILLIS)) }
        }
    }
    val blockedShake = remember(board) { Animatable(0f) }
    LaunchedEffect(lastTap) {
        val tap = lastTap ?: return@LaunchedEffect
        if (!tap.blocked || reduceMotion) return@LaunchedEffect
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
    val currentLit by rememberUpdatedState(lit)
    val currentLocked by rememberUpdatedState(locked)
    val currentHintCell by rememberUpdatedState(hintCell)

    BoxWithConstraints(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / board.cols, maxHeight / board.rows)
        val density = LocalDensity.current
        val cellSizePx = with(density) { cellSize.toPx() }
        val blockedCell = lastTap?.takeIf { it.blocked }?.cell

        Box(
            Modifier
                .size(cellSize * board.cols, cellSize * board.rows)
                .pointerInput(board, cellSizePx) {
                    fun cellAt(offset: Offset): Int? = resolveCircuitTap(board.rows, board.cols, offset.y / cellSizePx, offset.x / cellSizePx)
                    detectTapGestures(
                        onTap = { offset -> cellAt(offset)?.let(onTileTap) },
                        onLongPress = { offset -> cellAt(offset)?.let(onTileLongPress) },
                    )
                }
                .drawWithContent {
                    drawContent()
                    for (cell in 0 until board.size) {
                        val topLeft = Offset(board.col(cell) * cellSizePx, board.row(cell) * cellSizePx)
                        val shake = if (cell == blockedCell) blockedShake.value * cellSizePx * 0.06f else 0f
                        translate(left = shake) {
                            drawCircuitTile(
                                mask = startMasks[cell],
                                topLeft = topLeft,
                                cellSize = cellSizePx,
                                lit = currentLit[cell],
                                isSource = cell == board.source,
                                locked = currentLocked[cell],
                                palette = palette,
                                angle = angles[cell].value,
                                hinted = cell == currentHintCell,
                            )
                        }
                    }
                    if (board.wrap) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(cellSizePx * 0.18f, cellSizePx * 0.14f))
                        val frame = maxOf(1.dp.toPx(), cellSizePx * 0.025f)
                        drawRoundRect(
                            color = palette.frame,
                            topLeft = Offset(frame / 2, frame / 2),
                            size = Size(size.width - frame, size.height - frame),
                            cornerRadius = CornerRadius(cellSizePx * 0.2f),
                            style = Stroke(frame, pathEffect = dash),
                        )
                    }
                },
        )
    }
}
