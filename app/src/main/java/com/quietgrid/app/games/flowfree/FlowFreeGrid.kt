package com.quietgrid.app.games.flowfree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import kotlin.math.floor

private val FLOWFREE_COLOR_PALETTE = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835),
    Color(0xFF8E24AA), Color(0xFFFB8C00), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41),
)

private val FLOWFREE_PENCIL_PALETTE = listOf(
    Color(0xFF1A1A1A), Color(0xFF333333), Color(0xFF4D4D4D), Color(0xFF666666),
    Color(0xFF808080), Color(0xFF999999), Color(0xFFB3B3B3), Color(0xFFCCCCCC), Color(0xFFE0E0E0),
)

@Composable
fun FlowFreeGrid(
    puzzle: FlowFreePuzzleEntry,
    paths: Map<Int, List<Pair<Int, Int>>>,
    onDragStart: (row: Int, col: Int) -> Unit,
    onDragMove: (row: Int, col: Int) -> Unit,
    onDragEnd: () -> Unit,
) {
    val isPencilTheme = LocalIsPencilTheme.current
    val palette = if (isPencilTheme) FLOWFREE_PENCIL_PALETTE else FLOWFREE_COLOR_PALETTE
    val cellColorByPos = remember(paths) {
        paths.entries.flatMap { (color, cells) -> cells.map { it to color } }.toMap()
    }

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / puzzle.size, maxHeight / puzzle.size)
        val fontSize = (cellSize.value * 0.4f).sp

        fun cellAt(xDp: Dp, yDp: Dp): Pair<Int, Int>? {
            val col = floor(xDp / cellSize).toInt()
            val row = floor(yDp / cellSize).toInt()
            if (row < 0 || row >= puzzle.size || col < 0 || col >= puzzle.size) return null
            return row to col
        }

        Box(
            Modifier
                .size(cellSize * puzzle.size, cellSize * puzzle.size)
                .pointerInput(puzzle, cellSize) {
                    val density = this.density
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startCell = cellAt(Dp(down.position.x / density), Dp(down.position.y / density))
                        val pointerId = down.id
                        var lastCell = startCell
                        var dragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val cell = cellAt(Dp(change.position.x / density), Dp(change.position.y / density))
                            if (cell != null && cell != lastCell) {
                                if (!dragging && startCell != null) {
                                    onDragStart(startCell.first, startCell.second)
                                    dragging = true
                                }
                                onDragMove(cell.first, cell.second)
                                lastCell = cell
                            } else if (!dragging && startCell != null) {
                                onDragStart(startCell.first, startCell.second)
                                dragging = true
                            }
                            change.consume()
                        }
                        if (dragging) onDragEnd()
                    }
                },
        ) {
            for (row in 0 until puzzle.size) {
                for (col in 0 until puzzle.size) {
                    val color = cellColorByPos[row to col]
                    val endpointColor = puzzle.endpoints[row][col]
                    Box(
                        Modifier
                            .offset(cellSize * col, cellSize * row)
                            .size(cellSize)
                            .border(0.5.dp, Color.Gray)
                            .background(color?.let { palette[it % palette.size] } ?: Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (endpointColor != -1) {
                            BasicText(
                                text = (endpointColor + 1).toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
