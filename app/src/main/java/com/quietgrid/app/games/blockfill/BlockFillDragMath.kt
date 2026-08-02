package com.quietgrid.app.games.blockfill

import kotlin.math.floor

data class BlockFillDragAnchor(val row: Int, val col: Int)

/**
 * Resolves a raw pointer position (in the board's own pixel space) to the board cell that would
 * become the dragged piece's anchor (its cells[0] origin), or null if the piece wouldn't fully fit
 * there. Computed fresh from raw pixel inputs every call — never re-derived from an already-snapped
 * value — to avoid the compounding-quantization bug class that broke the old React Native version's
 * drag-and-drop (anchor math applied more than once across separate layers).
 */
fun resolveAnchorCell(
    pointerX: Float,
    pointerY: Float,
    boardOriginX: Float,
    boardOriginY: Float,
    cellSizePx: Float,
    board: BlockFillBoard,
    pieceCells: List<Pair<Int, Int>>,
): BlockFillDragAnchor? {
    if (cellSizePx <= 0f) return null
    val col = floor((pointerX - boardOriginX) / cellSizePx).toInt()
    val row = floor((pointerY - boardOriginY) / cellSizePx).toInt()
    if (!canPlacePieceAt(board, pieceCells, row, col)) return null
    return BlockFillDragAnchor(row, col)
}
