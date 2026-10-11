package com.quietgrid.app.games.killersudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.games.sudoku.sudokuBoxIndex
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.sudoku.SudokuGrid

private const val CAGE_INSET = 0.09f
private const val LABEL_WIDTH = 0.42f
private const val LABEL_HEIGHT = 0.3f
private const val NOTE_TOP = 0.2f

private fun Modifier.killerBoardLines(
    cellSizePx: Float,
    cageOfCell: IntArray,
    labelCells: Set<Int>,
    gridColor: Color,
    cageColor: Color,
): Modifier = drawWithContent {
    drawContent()
    val thin = 1.dp.toPx()
    val thick = 2.5.dp.toPx()
    for (i in 0..9) {
        val strokeWidth = if (i % 3 == 0) thick else thin
        val offset = cellSizePx * i
        drawLine(gridColor, Offset(offset, 0f), Offset(offset, size.height), strokeWidth)
        drawLine(gridColor, Offset(0f, offset), Offset(size.width, offset), strokeWidth)
    }

    val inset = cellSizePx * CAGE_INSET
    val stroke = 1.2.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))
    fun sameCage(cell: Int, rowStep: Int, colStep: Int): Boolean {
        val row = cell / 9 + rowStep
        val col = cell % 9 + colStep
        return row in 0..8 && col in 0..8 && cageOfCell[row * 9 + col] == cageOfCell[cell]
    }
    fun segment(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (x2 > x1 || y2 > y1) drawLine(cageColor, Offset(x1, y1), Offset(x2, y2), stroke, pathEffect = dash)
    }

    for (cell in 0 until 81) {
        val x0 = (cell % 9) * cellSizePx
        val y0 = (cell / 9) * cellSizePx
        val x1 = x0 + cellSizePx
        val y1 = y0 + cellSizePx
        val up = sameCage(cell, -1, 0)
        val down = sameCage(cell, 1, 0)
        val left = sameCage(cell, 0, -1)
        val right = sameCage(cell, 0, 1)
        val labelled = cell in labelCells
        val startX = if (left) x0 else x0 + inset
        val endX = if (right) x1 else x1 - inset
        val startY = if (up) y0 else y0 + inset
        val endY = if (down) y1 else y1 - inset

        if (!up) segment(if (labelled) maxOf(startX, x0 + cellSizePx * LABEL_WIDTH) else startX, y0 + inset, endX, y0 + inset)
        if (!down) segment(startX, y1 - inset, endX, y1 - inset)
        if (!left) segment(x0 + inset, if (labelled) maxOf(startY, y0 + cellSizePx * LABEL_HEIGHT) else startY, x0 + inset, endY)
        if (!right) segment(x1 - inset, startY, x1 - inset, endY)

        if (up && left && !sameCage(cell, -1, -1)) {
            segment(x0, y0 + inset, x0 + inset, y0 + inset)
            segment(x0 + inset, y0, x0 + inset, y0 + inset)
        }
        if (up && right && !sameCage(cell, -1, 1)) {
            segment(x1 - inset, y0 + inset, x1, y0 + inset)
            segment(x1 - inset, y0, x1 - inset, y0 + inset)
        }
        if (down && left && !sameCage(cell, 1, -1)) {
            segment(x0, y1 - inset, x0 + inset, y1 - inset)
            segment(x0 + inset, y1 - inset, x0 + inset, y1)
        }
        if (down && right && !sameCage(cell, 1, 1)) {
            segment(x1 - inset, y1 - inset, x1, y1 - inset)
            segment(x1 - inset, y1 - inset, x1 - inset, y1)
        }
    }
}

@Composable
private fun KillerCandidates(candidates: Set<Int>, cellSize: Dp, fontSize: TextUnit) {
    val slotWidth = cellSize / 3
    val slotHeight = cellSize * (1f - NOTE_TOP) / 3
    val style = TextStyle(
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Box(Modifier.size(cellSize)) {
        for (digit in candidates) {
            val slot = digit - 1
            Box(
                modifier = Modifier
                    .offset(x = slotWidth * (slot % 3), y = cellSize * NOTE_TOP + slotHeight * (slot / 3))
                    .size(slotWidth, slotHeight),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = digit.toString(), style = style, modifier = Modifier.wrapContentSize(unbounded = true))
            }
        }
    }
}

@Composable
fun KillerSudokuGrid(
    layout: KillerCageLayout,
    board: SudokuGrid,
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
    hint: KillerSudokuHint? = null,
) {
    val labelCells = remember(layout) { killerCageLabelCells(layout) }
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / 9, maxHeight / 9)
        val density = LocalDensity.current
        val fontSize = (cellSize.value * 0.5f).sp
        val noteFontSize = with(density) { (cellSize * 0.24f).toSp() }
        val sumFontSize = with(density) { (cellSize * 0.22f).toSp() }
        val cellSizePx = with(density) { cellSize.toPx() }
        val selectedRow = selectedCell?.first
        val selectedCol = selectedCell?.second
        val selectedBox = if (selectedRow != null && selectedCol != null) sudokuBoxIndex(selectedRow, selectedCol) else null
        val hintTargets = hint?.targetCells.orEmpty()
        val hintEvidence = hint?.evidenceCells.orEmpty()
        val hintCages = hint?.highlightCages.orEmpty()
        val hintRows = hint?.highlightRows.orEmpty()
        val hintCols = hint?.highlightCols.orEmpty()
        val hintBoxes = hint?.highlightBoxes.orEmpty()
        val hintDigit = (hint as? KillerSudokuPlacementHint)?.let { Triple(it.row, it.col, it.digit) }

        Box(
            Modifier
                .size(cellSize * 9, cellSize * 9)
                .killerBoardLines(
                    cellSizePx = cellSizePx,
                    cageOfCell = layout.cageOfCell,
                    labelCells = labelCells.keys,
                    gridColor = MaterialTheme.colorScheme.outlineVariant,
                    cageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    val cell = row * 9 + col
                    val value = board[row][col]
                    val locked = finishedCells[row][col]
                    val box = sudokuBoxIndex(row, col)
                    val cellKey = row to col
                    val isSelected = selectedCell == cellKey
                    val isPeerHighlight = !isSelected && selectedBox != null &&
                        (row == selectedRow || col == selectedCol || box == selectedBox)
                    val isCorrect = row in feedbackCorrectRows || col in feedbackCorrectCols || box in feedbackCorrectBoxes
                    val isIncorrect = row in feedbackIncorrectRows || col in feedbackIncorrectCols || box in feedbackIncorrectBoxes
                    val isHintTarget = cellKey in hintTargets
                    val isHintEvidence = cellKey in hintEvidence || layout.cageOfCell[cell] in hintCages
                    val isHintHighlight = !isHintTarget && !isHintEvidence && (row in hintRows || col in hintCols || box in hintBoxes)
                    val placementDigit = hintDigit?.takeIf { it.first == row && it.second == col }?.third

                    val backgroundColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isHintTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                        isHintEvidence -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                        isHintHighlight -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                        isPeerHighlight -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        value != null -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val emphasisColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isHintTarget -> MaterialTheme.colorScheme.tertiary
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (locked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                ),
                                isCorrect = isCorrect,
                                isIncorrect = isIncorrect,
                            )
                            placementDigit != null -> BasicText(
                                text = placementDigit.toString(),
                                style = TextStyle(
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            notes[row][col].isNotEmpty() -> KillerCandidates(notes[row][col], cellSize, noteFontSize)
                            else -> Unit
                        }
                        labelCells[cell]?.let { sum ->
                            BasicText(
                                text = sum.toString(),
                                style = TextStyle(
                                    fontSize = sumFontSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
