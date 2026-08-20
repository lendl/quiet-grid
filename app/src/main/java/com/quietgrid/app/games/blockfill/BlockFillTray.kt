package com.quietgrid.app.games.blockfill

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val TRAY_CELL_SIZE = 18.dp
private val TRAY_CELL_GAP = 2.dp

@Composable
fun BlockFillTray(
    tray: List<BlockFillPiece?>,
    draggingPieceIndex: Int?,
    onDragStart: (pieceIndex: Int, startPosition: Offset) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onPieceMeasured: (pieceIndex: Int, coordinates: LayoutCoordinates) -> Unit = { _, _ -> },
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tray.forEachIndexed { index, piece ->
            Box(
                Modifier
                    .size(TRAY_CELL_SIZE * 5)
                    .alpha(if (draggingPieceIndex == index) 0.25f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                if (piece != null) {
                    BlockFillPieceGlyph(
                        piece = piece,
                        modifier = Modifier
                            .onGloballyPositioned { coordinates -> onPieceMeasured(index, coordinates) }
                            .pointerInput(index, piece.shapeId) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    onDragStart(index, down.position)
                                    drag(down.id) { change ->
                                        change.consume()
                                        onDrag(change.positionChange())
                                    }
                                    onDragEnd()
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun BlockFillPieceGlyph(
    piece: BlockFillPiece,
    cellSize: Dp = TRAY_CELL_SIZE,
    cellGap: Dp = TRAY_CELL_GAP,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
) {
    val isPencil = LocalIsPencilTheme.current
    val color = if (isPencil) MaterialTheme.colorScheme.onSurface else blockFillFamilyColor(piece.family)
    val maxRow = piece.cells.maxOf { it.first }
    val maxCol = piece.cells.maxOf { it.second }
    val width = cellSize * (maxCol + 1)
    val height = cellSize * (maxRow + 1)

    Box(modifier.size(width, height)) {
        for ((row, col) in piece.cells) {
            BlockFillCell(
                color = color,
                size = cellSize - cellGap,
                modifier = Modifier.offset(x = cellSize * col, y = cellSize * row),
                elevated = elevated,
            )
        }
    }
}
