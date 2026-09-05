package com.quietgrid.app.games.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.engine.nonogram.NonogramGrid
import com.quietgrid.engine.nonogram.isNonogramLineComplete
import kotlin.math.floor

@Composable
fun NonogramBoard(
    puzzle: NonogramPuzzle,
    board: NonogramGrid,
    onTap: (row: Int, col: Int) -> Unit,
    onDragPaint: (cells: List<Pair<Int, Int>>) -> Unit,
    hintEvidenceCells: Set<Pair<Int, Int>> = emptySet(),
    hintTargetCells: Map<Pair<Int, Int>, Int> = emptyMap(),
) {
    val colClueDepth = puzzle.colClues.maxOf { it.size }
    val rowClueStrings = remember(puzzle) { puzzle.rowClues.map { it.joinToString(" ") } }
    val colClueStrings = remember(puzzle) { puzzle.colClues.map { it.joinToString("\n") } }
    val completedRows = remember(board, puzzle) {
        (0 until puzzle.rows).filter { r -> isNonogramLineComplete(board[r], puzzle.rowClues[r]) }.toSet()
    }
    val completedCols = remember(board, puzzle) {
        (0 until puzzle.cols).filter { c ->
            isNonogramLineComplete((0 until puzzle.rows).map { r -> board[r][c] }, puzzle.colClues[c])
        }.toSet()
    }

    val clueFontSize = 13.sp
    val clueColor = MaterialTheme.colorScheme.onSurfaceVariant
    val rowClueStyle = TextStyle(fontSize = clueFontSize, fontWeight = FontWeight.Bold, color = clueColor, textAlign = TextAlign.End)
    val colClueStyle = TextStyle(fontSize = clueFontSize, fontWeight = FontWeight.Bold, color = clueColor, textAlign = TextAlign.Center)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val rowClueAreaWidth = remember(rowClueStrings, rowClueStyle, maxWidth) {
            val widestPx = rowClueStrings.maxOf { textMeasurer.measure(it, rowClueStyle).size.width }
            (with(density) { widestPx.toDp() } + 12.dp).coerceAtMost(maxWidth * 0.35f)
        }
        val colClueAreaHeight = remember(colClueDepth, colClueStyle, maxHeight) {
            val sample = (1..colClueDepth).joinToString("\n") { "0" }
            val heightPx = textMeasurer.measure(sample, colClueStyle).size.height
            (with(density) { heightPx.toDp() } + 8.dp).coerceAtMost(maxHeight * 0.35f)
        }
        val cellSize = min(
            (maxWidth - rowClueAreaWidth) / puzzle.cols,
            (maxHeight - colClueAreaHeight) / puzzle.rows,
        )
        val gridX = rowClueAreaWidth
        val gridY = colClueAreaHeight
        val fontSize = (cellSize.value * 0.4f).sp

        fun cellAt(xDp: Dp, yDp: Dp): Pair<Int, Int>? {
            val col = floor(((xDp - gridX) / cellSize)).toInt()
            val row = floor(((yDp - gridY) / cellSize)).toInt()
            if (row < 0 || row >= puzzle.rows || col < 0 || col >= puzzle.cols) return null
            return row to col
        }

        Box(
            Modifier
                .size(gridX + cellSize * puzzle.cols, gridY + cellSize * puzzle.rows)
                .pointerInput(puzzle, cellSize) {
                    val touchSlop = viewConfiguration.touchSlop
                    val density = this.density
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startXDp = Dp(down.position.x / density)
                        val startYDp = Dp(down.position.y / density)
                        var dragging = false
                        val visited = mutableSetOf<Pair<Int, Int>>()
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val xDp = Dp(change.position.x / density)
                            val yDp = Dp(change.position.y / density)

                            if (!dragging) {
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (kotlin.math.sqrt(dx * dx + dy * dy) > touchSlop) {
                                    dragging = true
                                    cellAt(startXDp, startYDp)?.let {
                                        visited.add(it)
                                        onDragPaint(listOf(it))
                                    }
                                }
                            }
                            if (dragging) {
                                cellAt(xDp, yDp)?.let { cell ->
                                    if (visited.add(cell)) onDragPaint(listOf(cell))
                                }
                            }
                            change.consume()
                        }

                        if (!dragging) {
                            cellAt(startXDp, startYDp)?.let { (row, col) -> onTap(row, col) }
                        }
                    }
                },
        ) {
            rowClueStrings.forEachIndexed { rowIndex, text ->
                Box(
                    Modifier
                        .offset(x = 0.dp, y = gridY + cellSize * rowIndex)
                        .size(gridX, cellSize)
                        .alpha(if (rowIndex in completedRows) 0.3f else 1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    BasicText(
                        text = text,
                        style = rowClueStyle,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            colClueStrings.forEachIndexed { colIndex, text ->
                Box(
                    Modifier
                        .offset(x = gridX + cellSize * colIndex, y = 0.dp)
                        .size(cellSize, gridY)
                        .alpha(if (colIndex in completedCols) 0.3f else 1f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BasicText(text = text, style = colClueStyle)
                }
            }
            for (row in 0 until puzzle.rows) {
                for (col in 0 until puzzle.cols) {
                    val value = board[row][col]
                    val cellKey = row to col
                    val isHintEvidence = cellKey in hintEvidenceCells
                    val hintTargetValue = hintTargetCells[cellKey]
                    val backgroundColor = when {
                        value == 1 -> MaterialTheme.colorScheme.primary
                        hintTargetValue != null -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                        isHintEvidence -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val borderColor = if (hintTargetValue != null || isHintEvidence) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        Modifier
                            .offset(x = gridX + cellSize * col, y = gridY + cellSize * row)
                            .size(cellSize)
                            .background(backgroundColor, RoundedCornerShape(2.dp))
                            .border(if (hintTargetValue != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (value == 0) {
                            BasicText(
                                text = "×",
                                style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center),
                            )
                        } else if (value == null && hintTargetValue == 0) {
                            BasicText(
                                text = "×",
                                style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}
