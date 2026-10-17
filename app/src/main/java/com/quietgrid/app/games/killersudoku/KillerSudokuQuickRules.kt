package com.quietgrid.app.games.killersudoku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.games.sudoku.SUDOKU_MINI_CELL_GAP
import com.quietgrid.app.games.sudoku.SudokuMiniCell

private val CAGE_CELL_SIZE = 16.dp

@Composable
internal fun KillerClassicRulesPicture() {
    Column(verticalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
                for (col in 0 until 3) SudokuMiniCell(row * 3 + col + 1)
            }
        }
    }
}

@Composable
internal fun KillerCageSumPicture() {
    KillerMiniCage(sum = 10, digits = listOf(3, 7), conflictIndex = null)
}

@Composable
internal fun KillerCageRepeatPicture() {
    KillerMiniCage(sum = 8, digits = listOf(4, 4), conflictIndex = 1)
}

@Composable
private fun KillerMiniCage(sum: Int, digits: List<Int>, conflictIndex: Int?) {
    val cageColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        Box(
            Modifier
                .drawBehind {
                    drawRoundRect(
                        color = cageColor,
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx())),
                        ),
                    )
                }
                .padding(start = 3.dp, top = 10.dp, end = 3.dp, bottom = 3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
                digits.forEachIndexed { index, digit ->
                    SudokuMiniCell(digit, conflict = index == conflictIndex, size = CAGE_CELL_SIZE)
                }
            }
        }
        Text(
            sum.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 7.sp, fontWeight = FontWeight.Bold),
            color = cageColor,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 1.dp),
        )
    }
}
