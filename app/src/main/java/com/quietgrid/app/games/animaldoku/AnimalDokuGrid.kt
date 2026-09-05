// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuGrid.kt
package com.quietgrid.app.games.animaldoku

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlinx.coroutines.delay
import kotlin.math.floor

private const val DOUBLE_TAP_WINDOW_MS = 300L

private val REGION_PALETTE_LIGHT = listOf(
    Color(0xFF56B4E9), Color(0xFF0072B2), Color(0xFF009E73), Color(0xFFCC79A7),
    Color(0xFFE69F00), Color(0xFF882255), Color(0xFF9C7A00), Color(0xFF999999), Color(0xFFD55E00),
)

private val REGION_PALETTE_DARK = listOf(
    Color(0xFFE69F00), Color(0xFF56B4E9), Color(0xFF009E73), Color(0xFFF0E442),
    Color(0xFF0072B2), Color(0xFFD55E00), Color(0xFFCC79A7), Color(0xFF999999), Color(0xFF882255),
)

private fun offsetToCell(offset: Offset, cellSizePx: Float, size: Int): Pair<Int, Int>? {
    val col = floor(offset.x / cellSizePx).toInt()
    val row = floor(offset.y / cellSizePx).toInt()
    if (row !in 0 until size || col !in 0 until size) return null
    return row to col
}

@Composable
fun AnimalDokuGrid(
    size: Int,
    regions: List<List<Int>>,
    cells: List<List<AnimalDokuCellState>>,
    onCellTap: (Int, Int) -> Unit,
    onCellDrag: (markAll: Boolean, visited: List<Pair<Int, Int>>) -> Unit,
    onCellDoubleTap: (Int, Int) -> Unit,
) {
    val isPencilTheme = LocalIsPencilTheme.current
    val isDarkTheme = LocalIsDarkTheme.current
    val regionPalette = if (isDarkTheme) REGION_PALETTE_DARK else REGION_PALETTE_LIGHT
    val regionAlpha = if (isDarkTheme) 0.55f else 0.85f

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / size, maxHeight / size)
        val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }

        var lastTapCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var lastTapTimeMs by remember { mutableStateOf(0L) }
        val currentCells by rememberUpdatedState(cells)
        val borderColor = MaterialTheme.colorScheme.outline
        val borderStrokeWidth = 5.dp

        Box(
            Modifier
                .size(cellSize * size, cellSize * size)
                .pointerInput(size, cellSizePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pointerId = down.id
                        val startCell = offsetToCell(down.position, cellSizePx, size)
                        val startCellState = startCell?.let { (r, c) -> currentCells[r][c] }
                        val startCellDraggable = startCellState == AnimalDokuCellState.EMPTY || startCellState == AnimalDokuCellState.MARKED
                        val markAll = startCellState == AnimalDokuCellState.EMPTY
                        val visited = linkedSetOf<Pair<Int, Int>>()
                        startCell?.let { visited.add(it) }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val cell = offsetToCell(change.position, cellSizePx, size)
                            if (cell != null && startCell != null && startCellDraggable && visited.add(cell) && visited.size > 1) {
                                onCellDrag(markAll, visited.toList())
                            }
                            change.consume()
                        }

                        if (startCell == null) return@awaitEachGesture

                        if (visited.size <= 1) {
                            val now = System.currentTimeMillis()
                            if (lastTapCell == startCell && now - lastTapTimeMs < DOUBLE_TAP_WINDOW_MS) {
                                lastTapCell = null
                                onCellDoubleTap(startCell.first, startCell.second)
                            } else {
                                lastTapCell = startCell
                                lastTapTimeMs = now
                                onCellTap(startCell.first, startCell.second)
                            }
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (isPencilTheme) {
                        for (row in 0 until size) {
                            for (col in 0 until size) {
                                val region = regions[row][col]
                                val rightNeighborDifferent = col == size - 1 || regions[row][col + 1] != region
                                val bottomNeighborDifferent = row == size - 1 || regions[row + 1][col] != region
                                val x = cellSizePx * col
                                val y = cellSizePx * row
                                if (rightNeighborDifferent) {
                                    drawLine(borderColor, Offset(x + cellSizePx, y), Offset(x + cellSizePx, y + cellSizePx), strokeWidth = borderStrokeWidth.toPx())
                                }
                                if (bottomNeighborDifferent) {
                                    drawLine(borderColor, Offset(x, y + cellSizePx), Offset(x + cellSizePx, y + cellSizePx), strokeWidth = borderStrokeWidth.toPx())
                                }
                            }
                        }
                        drawRect(borderColor, style = Stroke(width = borderStrokeWidth.toPx()))
                    }
                },
        ) {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val region = regions[row][col]
                    val cellState = cells[row][col]
                    val backgroundColor = if (isPencilTheme) MaterialTheme.colorScheme.surface else regionPalette[region % regionPalette.size].copy(alpha = regionAlpha)

                    AnimalDokuAnimatedCell(
                        cellState = cellState,
                        region = region,
                        cellSize = cellSize,
                        backgroundColor = backgroundColor,
                        isPencilTheme = isPencilTheme,
                        modifier = Modifier.offset(x = cellSize * col, y = cellSize * row),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimalDokuAnimatedCell(
    cellState: AnimalDokuCellState,
    region: Int,
    cellSize: Dp,
    backgroundColor: Color,
    isPencilTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val squish = remember { Animatable(1f) }
    var previousState by remember { mutableStateOf<AnimalDokuCellState?>(null) }
    LaunchedEffect(cellState) {
        if (previousState != null && previousState != cellState) {
            squish.snapTo(1f)
            squish.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 220
                    1f at 0
                    0.88f at 70
                    1.05f at 140
                    1f at 220
                },
            )
        }
        previousState = cellState
    }

    Box(
        modifier
            .size(cellSize)
            .graphicsLayer(scaleX = squish.value, scaleY = squish.value)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(percent = 22))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = cellState,
            transitionSpec = {
                when {
                    initialState == AnimalDokuCellState.EMPTY && targetState == AnimalDokuCellState.MARKED ->
                        (scaleIn(initialScale = 0.3f, animationSpec = tween(180)) + fadeIn(tween(180))) togetherWith
                            (scaleOut(targetScale = 0.3f, animationSpec = tween(100)) + fadeOut(tween(100)))
                    initialState == AnimalDokuCellState.MARKED && targetState == AnimalDokuCellState.EMPTY ->
                        fadeIn(tween(1)) togetherWith
                            (scaleOut(targetScale = 0.3f, animationSpec = tween(180)) + fadeOut(tween(180)))
                    targetState == AnimalDokuCellState.LOCKED_WRONG ->
                        (
                            scaleIn(
                                initialScale = 0.4f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            ) + fadeIn(tween(120))
                            ) togetherWith fadeOut(tween(100))
                    else -> fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                }
            },
            label = "animaldoku-cell-content",
        ) { state ->
            when (state) {
                AnimalDokuCellState.EMPTY -> Unit
                AnimalDokuCellState.MARKED -> AnimalDokuXMark(cellSize, locked = false, isPencilTheme = isPencilTheme)
                AnimalDokuCellState.LOCKED_WRONG -> AnimalDokuXMark(cellSize, locked = true, isPencilTheme = isPencilTheme)
                AnimalDokuCellState.LOCKED_CORRECT -> AnimalDokuCorrectReveal(region, cellSize)
            }
        }
    }
}

@Composable
private fun AnimalDokuCorrectReveal(region: Int, cellSize: Dp) {
    var settled by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        delay(110)
        settled = true
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    AnimalDokuAnimalIcon(
        pose = if (settled) AnimalDokuAnimalPose.FRONT_FACING else AnimalDokuAnimalPose.PEEKING,
        region = region,
        modifier = Modifier.size(cellSize * 0.7f).graphicsLayer(scaleX = scale.value, scaleY = scale.value),
    )
}

@Composable
private fun AnimalDokuXMark(cellSize: androidx.compose.ui.unit.Dp, locked: Boolean, isPencilTheme: Boolean) {
    val color = when {
        isPencilTheme -> Color.Black
        locked -> Color(0xFFD9534F)
        else -> Color.White
    }
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(cellSize * 0.75f),
    )
}
