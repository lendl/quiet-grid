package com.quietgrid.app.games.takuzu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.components.FeedbackText

private val GAP = 2.dp

@Composable
fun TakuzuBoard(
    board: TakuzuGrid,
    isGiven: List<List<Boolean>>,
    finishedCells: List<List<Boolean>>,
    feedbackCorrectRows: Set<Int>,
    feedbackCorrectCols: Set<Int>,
    feedbackIncorrectRows: Set<Int>,
    feedbackIncorrectCols: Set<Int>,
    size: Int,
    onCellPress: (row: Int, col: Int) -> Unit,
    hintEvidenceCells: Set<Pair<Int, Int>> = emptySet(),
    hintTargetCells: Map<Pair<Int, Int>, Int> = emptyMap(),
    hintHighlightRows: Set<Int> = emptySet(),
    hintHighlightCols: Set<Int> = emptySet(),
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cellSize = min(
            (maxWidth - GAP * (size - 1)) / size,
            (maxHeight - GAP * (size - 1)) / size,
        )
        val borderRadius = cellSize * 0.14f
        val fontSize = (cellSize.value * 0.44f).sp
        val stride = cellSize + GAP
        val gridExtent = stride * size - GAP

        Box(Modifier.size(gridExtent, gridExtent)) {
        for (row in 0 until size) {
            for (col in 0 until size) {
                val value = board[row][col]
                val given = isGiven[row][col]
                val locked = given || finishedCells[row][col]
                val isCorrect = row in feedbackCorrectRows || col in feedbackCorrectCols
                val isIncorrect = row in feedbackIncorrectRows || col in feedbackIncorrectCols
                val cellKey = row to col
                val hintTargetValue = hintTargetCells[cellKey]
                val isHintTarget = hintTargetValue != null
                val isHintEvidence = cellKey in hintEvidenceCells
                val isHintHighlight = !isHintEvidence && !isHintTarget &&
                    (row in hintHighlightRows || col in hintHighlightCols)

                val backgroundColor = when {
                    isHintTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                    isHintEvidence -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                    isHintHighlight -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                    locked -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = if (isHintTarget || isHintEvidence) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                val textColor = if (given) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .offset(x = stride * col, y = stride * row)
                        .size(cellSize)
                        .clickable(enabled = !locked) { onCellPress(row, col) }
                        .background(backgroundColor, RoundedCornerShape(borderRadius))
                        .border(if (isHintTarget || isHintEvidence) 2.dp else 1.dp, borderColor, RoundedCornerShape(borderRadius)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        value != null -> FeedbackText(
                            text = value.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = if (locked) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                            ),
                            isCorrect = isCorrect,
                            isIncorrect = isIncorrect,
                        )
                        hintTargetValue != null -> BasicText(
                            text = hintTargetValue.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
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
