package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.floor

@Composable
fun WordSearchGrid(
    puzzle: WordSearchPuzzleEntry,
    foundWordIds: List<String>,
    tempSelection: WSSelection?,
    hiddenWordMode: Boolean,
    hiddenWordProgress: List<WSCellRef>,
    onCellTap: (row: Int, col: Int) -> Unit,
    onHiddenWordTap: (row: Int, col: Int) -> Unit,
    nextMoveEvidenceCells: List<WSCellRef> = emptyList(),
    nextMoveTargetCells: List<WSCellRef> = emptyList(),
) {
    val foundCells = remember(foundWordIds, puzzle) {
        puzzle.words.filter { it.id in foundWordIds }.flatMap { it.positions }.toSet()
    }
    val selectedCells = remember(tempSelection) { tempSelection?.path?.toSet() ?: emptySet() }
    val hiddenProgressCells = remember(hiddenWordProgress) { hiddenWordProgress.toSet() }
    val selectionStart = tempSelection?.path?.firstOrNull()
    val selectionEnd = tempSelection?.path?.lastOrNull()
    val evidenceCellSet = remember(nextMoveEvidenceCells) { nextMoveEvidenceCells.toSet() }
    val targetCellSet = remember(nextMoveTargetCells) { nextMoveTargetCells.toSet() }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / puzzle.cols, maxHeight / puzzle.rows)
        val fontSize = (cellSize.value * 0.42f).sp

        fun cellAt(xDp: Dp, yDp: Dp): WSCellRef? {
            val col = floor(xDp / cellSize).toInt()
            val row = floor(yDp / cellSize).toInt()
            if (row < 0 || row >= puzzle.rows || col < 0 || col >= puzzle.cols) return null
            return WSCellRef(row, col)
        }

        Box(
            Modifier
                .size(cellSize * puzzle.cols, cellSize * puzzle.rows)
                .pointerInput(puzzle, hiddenWordMode, cellSize) {
                    val density = this.density
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startXDp = Dp(down.position.x / density)
                        val startYDp = Dp(down.position.y / density)
                        val startCell = cellAt(startXDp, startYDp)
                        val pointerId = down.id
                        var claimedByPan = false

                        if (hiddenWordMode && startCell != null) {
                            onHiddenWordTap(startCell.row, startCell.col)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (change.isConsumed) claimedByPan = true
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                        }

                        // Selection is tap-only (matching the RN app): this frees up single-finger
                        // drag for panning the board once zoomed in, instead of the two conflicting.
                        if (!hiddenWordMode && !claimedByPan) {
                            startCell?.let { onCellTap(it.row, it.col) }
                        }
                    }
                },
        ) {
            for (row in 0 until puzzle.rows) {
                for (col in 0 until puzzle.cols) {
                    val cell = WSCellRef(row, col)
                    val letter = puzzle.grid[row][col]
                    val isFound = cell in foundCells
                    val isSelected = cell in selectedCells
                    val isSelectionEndpoint = cell == selectionStart || cell == selectionEnd
                    val isHiddenProgress = cell in hiddenProgressCells
                    val isInCrosshair = !isSelected && !isFound && !isHiddenProgress &&
                        selectionStart != null && (cell.row == selectionStart.row || cell.col == selectionStart.col)
                    val isHintCell = cell in evidenceCellSet || cell in targetCellSet

                    val backgroundColor = when {
                        isSelected && isSelectionEndpoint -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        isSelected -> MaterialTheme.colorScheme.primary
                        isHiddenProgress -> MaterialTheme.colorScheme.secondary
                        isFound -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                        isHintCell -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                        isInCrosshair -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize)
                            .background(backgroundColor, RoundedCornerShape(4.dp))
                            .then(
                                if (isHintCell) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = letter,
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected || isHiddenProgress) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            }
        }
    }
}
