package com.quietgrid.shared

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.engine.sudoku.SUDOKU_BOX_SIZE
import com.quietgrid.engine.sudoku.SUDOKU_SIZE
import com.quietgrid.shared.resources.Res
import com.quietgrid.shared.resources.skeleton_title
import org.jetbrains.compose.resources.stringResource

private val TitleSpace = 96.dp
private val MaxCellSize = 48.dp

@Composable
fun SkeletonScreen(modifier: Modifier = Modifier) {
    val solved = remember { solvedSkeletonSudoku() }
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cellSize = min(min(maxWidth, maxHeight - TitleSpace) / SUDOKU_SIZE, MaxCellSize)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.skeleton_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    SkeletonGrid(solved, cellSize, MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SkeletonGrid(solved: List<List<Int>>, cellSize: Dp, lineColor: Color) {
    Column(Modifier.border(2.dp, lineColor)) {
        repeat(SUDOKU_BOX_SIZE) { boxRow ->
            Row {
                repeat(SUDOKU_BOX_SIZE) { boxCol ->
                    Column(Modifier.border(1.dp, lineColor)) {
                        repeat(SUDOKU_BOX_SIZE) { rowInBox ->
                            Row {
                                repeat(SUDOKU_BOX_SIZE) { colInBox ->
                                    val row = boxRow * SUDOKU_BOX_SIZE + rowInBox
                                    val col = boxCol * SUDOKU_BOX_SIZE + colInBox
                                    SkeletonCell(
                                        digit = solved[row][col],
                                        given = SKELETON_PUZZLE[row * SUDOKU_SIZE + col] != '0',
                                        size = cellSize,
                                        lineColor = lineColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonCell(digit: Int, given: Boolean, size: Dp, lineColor: Color) {
    Box(
        modifier = Modifier.size(size).border(0.5.dp, lineColor.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            fontWeight = if (given) FontWeight.Bold else FontWeight.Normal,
            color = if (given) lineColor else MaterialTheme.colorScheme.primary,
        )
    }
}
