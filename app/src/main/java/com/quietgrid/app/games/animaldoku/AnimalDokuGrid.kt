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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlinx.coroutines.delay
import kotlin.math.floor

private const val DOUBLE_TAP_WINDOW_MS = 300L

/**
 * Okabe-Ito colorblind-safe categorical palette (the 8-color reference set used throughout
 * accessibility-conscious data visualization), extended with one additional maroon for AnimalDoku's
 * 9-region ceiling. Chosen specifically because every prior ad hoc palette drifted into same-family
 * near-duplicates (blue vs indigo, green vs olive, purple vs indigo) that were hard to tell apart
 * once alpha-blended over the cell background -- this set is built to stay perceptually distinct
 * even under that blending, not just distinct at full saturation.
 */
private val REGION_PALETTE = listOf(
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

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / size, maxHeight / size)
        val fontSize = (cellSize.value * 0.32f).sp
        val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }

        var lastTapCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var lastTapTimeMs by remember { mutableStateOf(0L) }
        // Pencil theme has no color channel to lean on, so region boundaries stay at full strength
        // there -- they're the only way to tell regions apart. In the color theme, the palette
        // itself now carries that job, so the drawn lines are just a light structural hint.
        val borderColor = if (isPencilTheme) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        val borderStrokeWidth = if (isPencilTheme) 3.dp else 1.dp

        Box(
            Modifier
                .size(cellSize * size, cellSize * size)
                .pointerInput(size, cellSizePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pointerId = down.id
                        val startCell = offsetToCell(down.position, cellSizePx, size)
                        // Captured once, at finger-down, and reused unchanged for every cell entered
                        // during this gesture. onCellDrag fires once per newly-entered cell (so marks
                        // appear live while dragging), and each firing mutates the session -- if the
                        // batch direction were re-derived from the live cell state on every firing
                        // instead of locked in here, the direction would flip mid-swipe the moment the
                        // start cell's own state changed from the first firing.
                        val startCellState = startCell?.let { (r, c) -> cells[r][c] }
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
                            // Report every newly-entered cell immediately so marks appear live while
                            // dragging, not only once the finger lifts. visited.add returns false for
                            // cells already in the set, so re-crossing old cells doesn't re-fire.
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
                },
        ) {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val region = regions[row][col]
                    val cellState = cells[row][col]
                    val backgroundColor = if (isPencilTheme) MaterialTheme.colorScheme.surface else REGION_PALETTE[region % REGION_PALETTE.size].copy(alpha = 0.55f)

                    AnimalDokuAnimatedCell(
                        cellState = cellState,
                        region = region,
                        cellSize = cellSize,
                        backgroundColor = backgroundColor,
                        modifier = Modifier.offset(x = cellSize * col, y = cellSize * row),
                    ) {
                        if (isPencilTheme) {
                            BasicText(
                                text = (region + 1).toString(),
                                style = TextStyle(fontSize = fontSize * 0.6f, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center),
                                modifier = Modifier.align(Alignment.TopStart).offset(x = 2.dp, y = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One cell's box, background, region-number overlay (via [pencilOverlay]), and the mark/animal
 * content itself -- with the full one-shot choreography: mark grows in from center, unmark shrinks
 * back out, a correct open makes the animal jump out then settle, a wrong open pops the X in, and
 * any state change at all also triggers a quick universal corner-squish on the whole cell. Every
 * animation here is one-shot (finite duration, never looping/repeating) matching this app's
 * established animation convention.
 */
@Composable
private fun AnimalDokuAnimatedCell(
    cellState: AnimalDokuCellState,
    region: Int,
    cellSize: Dp,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    pencilOverlay: @Composable () -> Unit,
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
        pencilOverlay()
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
                AnimalDokuCellState.MARKED -> AnimalDokuXMark(cellSize, locked = false)
                AnimalDokuCellState.LOCKED_WRONG -> AnimalDokuXMark(cellSize, locked = true)
                AnimalDokuCellState.LOCKED_CORRECT -> AnimalDokuCorrectReveal(region, cellSize)
            }
        }
    }
}

/**
 * The correct-open reveal: the animal briefly peeks (small, bottom-aligned) then jumps up and
 * settles front-facing (full size, centered) with a light bounce -- a one-shot, two-stage motion
 * that never repeats once settled.
 */
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
private fun AnimalDokuXMark(cellSize: androidx.compose.ui.unit.Dp, locked: Boolean) {
    val color = if (locked) Color(0xFFD9534F) else Color.White
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(cellSize * 0.75f),
    )
}
