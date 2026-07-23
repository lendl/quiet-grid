package com.quietgrid.app.games.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.components.FeedbackText

/**
 * Draws the classic thick 3x3 box-separator lines and outer frame as a single overlay spanning
 * the whole board, instead of per-cell borders (which can only draw a uniform border around one
 * cell at a time and can't produce a line shared cleanly between two neighboring cells).
 */
private fun Modifier.sudokuBoxLines(cellSizePx: Float, color: androidx.compose.ui.graphics.Color): Modifier = drawWithContent {
    drawContent()
    val thin = 1.dp.toPx()
    val thick = 2.5.dp.toPx()
    for (i in 0..9) {
        val strokeWidth = if (i % 3 == 0) thick else thin
        val x = cellSizePx * i
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
        val y = cellSizePx * i
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
    }
}

@Composable
fun SudokuGrid(
    board: SudokuGrid,
    givens: List<List<Int?>>,
    notes: List<List<Set<Int>>>,
    finishedCells: List<List<Boolean>>,
    selectedCell: Pair<Int, Int>?,
    feedbackCorrectRows: Set<Int>,
    feedbackCorrectCols: Set<Int>,
    feedbackCorrectBoxes: Set<Int>,
    feedbackIncorrectRows: Set<Int>,
    feedbackIncorrectCols: Set<Int>,
    feedbackIncorrectBoxes: Set<Int>,
    onCellPress: (row: Int, col: Int) -> Unit,
    hintEvidenceCells: Set<Pair<Int, Int>> = emptySet(),
    hintTargetCells: Set<Pair<Int, Int>> = emptySet(),
    hintPlacementTarget: Triple<Int, Int, Int>? = null,
    hintHighlightRows: Set<Int> = emptySet(),
    hintHighlightCols: Set<Int> = emptySet(),
    hintHighlightBoxes: Set<Int> = emptySet(),
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / 9, maxHeight / 9)
        val noteFontSize = (cellSize.value * 0.22f).sp
        val fontSize = (cellSize.value * 0.5f).sp

        val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }
        Box(
            Modifier
                .size(cellSize * 9, cellSize * 9)
                .sudokuBoxLines(cellSizePx, MaterialTheme.colorScheme.outline),
        ) {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val value = board[row][col]
                val given = givens[row][col] != null
                val locked = given || finishedCells[row][col]
                val box = sudokuBoxIndex(row, col)
                val isSelected = selectedCell == row to col
                val isCorrect = row in feedbackCorrectRows || col in feedbackCorrectCols || box in feedbackCorrectBoxes
                val isIncorrect = row in feedbackIncorrectRows || col in feedbackIncorrectCols || box in feedbackIncorrectBoxes
                val cellKey = row to col
                val isHintTarget = cellKey in hintTargetCells
                val isHintEvidence = cellKey in hintEvidenceCells
                val isHintHighlight = !isHintTarget && !isHintEvidence &&
                    (row in hintHighlightRows || col in hintHighlightCols || box in hintHighlightBoxes)
                val hintDigit = hintPlacementTarget?.takeIf { it.first == row && it.second == col }?.third

                val backgroundColor = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isHintTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                    isHintEvidence -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                    isHintHighlight -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                    locked -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
                val emphasisColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isHintTarget || isHintEvidence -> MaterialTheme.colorScheme.tertiary
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .offset(x = cellSize * col, y = cellSize * row)
                        .size(cellSize)
                        .clickable(enabled = !locked) { onCellPress(row, col) }
                        .background(backgroundColor)
                        .then(if (emphasisColor != null) Modifier.border(2.dp, emphasisColor) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        value != null -> FeedbackText(
                            text = value.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = if (given) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (given) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            ),
                            isCorrect = isCorrect,
                            isIncorrect = isIncorrect,
                        )
                        hintDigit != null -> BasicText(
                            text = hintDigit.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        notes[row][col].isNotEmpty() -> BasicText(
                            text = (1..9).joinToString("") { if (it in notes[row][col]) it.toString() else " " }
                                .chunked(3).joinToString("\n"),
                            style = TextStyle(
                                fontSize = noteFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        else -> Unit
                    }
                }
            }
        }
        }
    }
}
