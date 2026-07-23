package com.quietgrid.app.games.chimptest

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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

private val GAP = 6.dp
private val WRONG_TAP_BORDER = Color(0xFFEF4444)

@Composable
fun ChimpTestGrid(
    cells: List<ChimpTestCell>,
    revealAll: Boolean,
    wrongTapCell: Int?,
    gridSize: Int,
    nextExpected: Int,
    onCellTap: (row: Int, col: Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cellSize = min(
            (maxWidth - GAP * (gridSize - 1)) / gridSize,
            (maxHeight - GAP * (gridSize - 1)) / gridSize,
        )
        val borderRadius = cellSize * 0.16f
        val fontSize = (cellSize.value * 0.44f).sp
        val stride = cellSize + GAP
        val gridExtent = stride * gridSize - GAP

        Box(Modifier.size(gridExtent, gridExtent)) {
        cells.forEach { cell ->
            val isRemoved = cell.hidden && !revealAll && cell.number < nextExpected
            if (isRemoved) return@forEach

            key(cell.number) {
                val isMemoryPhase = cell.hidden && !revealAll
                val isWrongCell = revealAll && cell.number == wrongTapCell
                val borderColor = if (isWrongCell) WRONG_TAP_BORDER else MaterialTheme.colorScheme.primary
                val borderWidth = if (isWrongCell) 4.dp else 3.dp

                Box(
                    modifier = Modifier
                        .offset(x = stride * cell.col, y = stride * cell.row)
                        .size(cellSize)
                        .clickable { onCellTap(cell.row, cell.col) }
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(borderRadius))
                        .border(borderWidth, borderColor, RoundedCornerShape(borderRadius)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isMemoryPhase) {
                        BasicText(
                            text = cell.number.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
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
