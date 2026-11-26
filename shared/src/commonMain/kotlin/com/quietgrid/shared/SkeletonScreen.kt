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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.engine.sudoku.SUDOKU_BOX_SIZE
import com.quietgrid.engine.sudoku.SUDOKU_SIZE
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.shared.resources.Res
import com.quietgrid.shared.resources.daily_title
import com.quietgrid.shared.resources.settings_keep_screen_on_label
import com.quietgrid.shared.resources.skeleton_title
import com.quietgrid.shared.resources.wordsearch_next_move_title
import org.jetbrains.compose.resources.stringResource

private val TextSpace = 192.dp
private val MaxCellSize = 48.dp

@Composable
fun SkeletonScreen(modifier: Modifier = Modifier) {
    val entry by produceState<SudokuPuzzleEntry?>(null) {
        value = firstSudokuEntry(Res.readBytes(SKELETON_BANK_FILE).decodeToString())
    }
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cellSize = min(min(maxWidth, maxHeight - TextSpace) / SUDOKU_SIZE, MaxCellSize)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.skeleton_title), style = MaterialTheme.typography.headlineMedium)
                    entry?.let { loaded ->
                        Spacer(Modifier.height(16.dp))
                        SkeletonGrid(loaded, cellSize, MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(Res.string.daily_title))
                        Text(stringResource(Res.string.settings_keep_screen_on_label))
                        Text(stringResource(Res.string.wordsearch_next_move_title, "CAT"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonGrid(entry: SudokuPuzzleEntry, cellSize: Dp, lineColor: Color) {
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
                                        digit = entry.solution[row][col],
                                        given = entry.givens[row][col] != null,
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
