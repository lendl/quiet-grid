package com.quietgrid.app.games.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SUDOKU_MINI_CELL_SIZE = 12.dp
internal val SUDOKU_MINI_CELL_GAP = 2.dp
private val MINI_CELL_SHAPE = RoundedCornerShape(3.dp)

@Composable
internal fun SudokuRowRulePicture(first: Int, middle: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
        SudokuMiniCell(first)
        SudokuMiniCell(middle)
        SudokuMiniCell(first, conflict = true)
    }
}

@Composable
internal fun SudokuColumnRulePicture(first: Int, middle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
        SudokuMiniCell(first)
        SudokuMiniCell(middle)
        SudokuMiniCell(first, conflict = true)
    }
}

@Composable
internal fun SudokuBoxRulePicture(boxRows: Int, boxCols: Int, digit: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
        for (row in 0 until boxRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(SUDOKU_MINI_CELL_GAP)) {
                for (col in 0 until boxCols) {
                    when {
                        row == 0 && col == 0 -> SudokuMiniCell(digit)
                        row == boxRows - 1 && col == boxCols - 1 -> SudokuMiniCell(digit, conflict = true)
                        else -> SudokuMiniCell(null)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SudokuMiniCell(value: Int?, conflict: Boolean = false, size: Dp = SUDOKU_MINI_CELL_SIZE) {
    val error = MaterialTheme.colorScheme.error
    Box(
        Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surface, MINI_CELL_SHAPE)
            .border(if (conflict) 1.5.dp else 1.dp, if (conflict) error else MaterialTheme.colorScheme.outlineVariant, MINI_CELL_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        if (value != null) {
            val textSize = (size.value * 0.62f).sp
            Text(
                value.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = textSize, lineHeight = textSize, fontWeight = FontWeight.Bold),
                color = if (conflict) error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
