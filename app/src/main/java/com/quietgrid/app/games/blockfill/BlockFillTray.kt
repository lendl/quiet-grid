package com.quietgrid.app.games.blockfill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TRAY_CELL_SIZE = 17.dp
private val TRAY_CELL_GAP = 1.dp
private val TRAY_CORNER_RADIUS = 14.dp
private val TRAY_BORDER_WIDTH = 1.dp
private const val DRAGGING_SLOT_ALPHA = 0.25f
internal const val UNPLACEABLE_PIECE_ALPHA = 0.35f
private const val DIAGONAL_BRIDGE_ALPHA = 0.6f
private const val DIAGONAL_BRIDGE_WIDTH_FRACTION = 0.22f

internal fun diagonalBridges(cells: List<Pair<Int, Int>>): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
    val cellSet = cells.toSet()
    return cells.flatMap { (row, col) ->
        listOf(row + 1 to col + 1, row + 1 to col - 1)
            .filter { diagonal -> diagonal in cellSet && (row + 1 to col) !in cellSet && (row to diagonal.second) !in cellSet }
            .map { diagonal -> (row to col) to diagonal }
    }
}

@Composable
fun BlockFillTray(
    tray: List<BlockFillPiece?>,
    placeablePieces: List<Boolean>,
    draggingPieceIndex: Int?,
    onDragStart: (pieceIndex: Int, startPosition: Offset) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onSlotMeasured: (pieceIndex: Int, coordinates: LayoutCoordinates) -> Unit = { _, _ -> },
) {
    val trayShape = RoundedCornerShape(TRAY_CORNER_RADIUS)
    val isDark = blockFillIsDarkSurface()
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(trayShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isDark) Modifier else Modifier.border(TRAY_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant, trayShape))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tray.forEachIndexed { index, piece ->
            val placeable = piece != null && placeablePieces.getOrElse(index) { true }
            Box(
                Modifier
                    .size(TRAY_CELL_SIZE * 5)
                    .alpha(
                        when {
                            draggingPieceIndex == index -> DRAGGING_SLOT_ALPHA
                            piece != null && !placeable -> UNPLACEABLE_PIECE_ALPHA
                            else -> 1f
                        },
                    )
                    .onGloballyPositioned { coordinates -> onSlotMeasured(index, coordinates) }
                    .then(
                        if (piece != null && placeable) {
                            Modifier.pointerInput(index, piece.shapeId) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    onDragStart(index, down.position)
                                    var released = false
                                    try {
                                        released = drag(down.id) { change ->
                                            val delta = change.positionChange()
                                            change.consume()
                                            onDrag(delta)
                                        }
                                    } finally {
                                        if (released) onDragEnd() else onDragCancel()
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (piece != null) {
                    BlockFillPieceGlyph(piece = piece)
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
) {
    val color = blockFillFamilyColor(piece.family)
    val maxRow = piece.cells.maxOf { it.first }
    val maxCol = piece.cells.maxOf { it.second }
    val width = cellSize * (maxCol + 1)
    val height = cellSize * (maxRow + 1)
    val inset = cellGap / 2
    val bridges = remember(piece.cells) { diagonalBridges(piece.cells) }

    Box(
        modifier
            .size(width, height)
            .drawBehind {
                val pitch = cellSize.toPx()
                for ((from, to) in bridges) {
                    drawLine(
                        color = color.copy(alpha = DIAGONAL_BRIDGE_ALPHA),
                        start = Offset(pitch * (from.second + 0.5f), pitch * (from.first + 0.5f)),
                        end = Offset(pitch * (to.second + 0.5f), pitch * (to.first + 0.5f)),
                        strokeWidth = pitch * DIAGONAL_BRIDGE_WIDTH_FRACTION,
                        cap = StrokeCap.Round,
                    )
                }
            },
    ) {
        for ((row, col) in piece.cells) {
            BlockFillCell(
                color = color,
                size = cellSize - cellGap,
                modifier = Modifier.offset(x = cellSize * col + inset, y = cellSize * row + inset),
            )
        }
    }
}
