package com.quietgrid.app.games.blockfill

import kotlin.math.floor

data class BlockFillDragAnchor(val row: Int, val col: Int)

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
