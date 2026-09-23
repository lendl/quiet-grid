package com.quietgrid.app.games.battleship

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.engine.battleship.BattleshipBoard
import com.quietgrid.engine.battleship.BattleshipCell

private val MAX_BOARD_CELL_SIZE = 32.dp
private const val CLUE_TO_BOARD_CELL_RATIO = 0.875f

@Composable
fun BattleshipBoard(
    board: BattleshipBoard,
    rowClues: List<Int>,
    colClues: List<Int>,
    givenCells: Set<Pair<Int, Int>>,
    onCellPress: (Int, Int) -> Unit,
) {
    val size = board.size
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val slots = size + CLUE_TO_BOARD_CELL_RATIO
        val cellSize = min(MAX_BOARD_CELL_SIZE, min(maxWidth / slots, maxHeight / slots))
        val clueSize = cellSize * CLUE_TO_BOARD_CELL_RATIO
        Column {
            Row {
                Box(Modifier.size(clueSize))
                colClues.forEach { clue ->
                    Box(Modifier.size(clueSize), contentAlignment = Alignment.Center) {
                        Text(text = clue.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            for (row in 0 until size) {
                Row {
                    Box(Modifier.size(clueSize), contentAlignment = Alignment.Center) {
                        Text(text = rowClues[row].toString(), style = MaterialTheme.typography.labelSmall)
                    }
                    for (col in 0 until size) {
                        BattleshipCellView(
                            cell = board[row][col],
                            isGiven = (row to col) in givenCells,
                            cellSize = cellSize,
                            onPress = { onCellPress(row, col) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleshipCellView(cell: BattleshipCell, isGiven: Boolean, cellSize: Dp, onPress: () -> Unit) {
    val backgroundColor = when (cell) {
        BattleshipCell.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
        BattleshipCell.WATER -> MaterialTheme.colorScheme.primaryContainer
        BattleshipCell.SHIP -> MaterialTheme.colorScheme.secondary
    }
    val borderColor = if (isGiven) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(cellSize)
            .border(width = if (isGiven) 2.dp else 1.dp, color = borderColor)
            .background(backgroundColor)
            .clickable(enabled = !isGiven, onClick = onPress),
    )
}

@Composable
fun BattleshipFleetStrip(remainingFleetCounts: Map<Int, Int>) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        remainingFleetCounts.toSortedMap().forEach { (length, count) ->
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = "${"S".repeat(length)} x$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (count <= 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
