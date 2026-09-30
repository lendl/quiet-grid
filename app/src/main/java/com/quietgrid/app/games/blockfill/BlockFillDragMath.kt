package com.quietgrid.app.games.blockfill

import kotlin.math.floor

data class BlockFillDragAnchor(val row: Int, val col: Int)

data class BlockFillPiecePosition(val x: Float, val y: Float)

fun floatingPieceTopLeft(
    pointerX: Float,
    pointerY: Float,
    pieceCells: List<Pair<Int, Int>>,
    cellSizePx: Float,
    liftPx: Float,
): BlockFillPiecePosition {
    val width = cellSizePx * (pieceCells.maxOf { it.second } + 1)
    val height = cellSizePx * (pieceCells.maxOf { it.first } + 1)
    return BlockFillPiecePosition(x = pointerX - width / 2f, y = pointerY - height - liftPx)
}

fun resolveAnchorCell(
    pieceLeftX: Float,
    pieceTopY: Float,
    boardOriginX: Float,
    boardOriginY: Float,
    cellSizePx: Float,
    board: BlockFillBoard,
    pieceCells: List<Pair<Int, Int>>,
): BlockFillDragAnchor? {
    if (cellSizePx <= 0f) return null
    val col = floor((pieceLeftX - boardOriginX) / cellSizePx + 0.5f).toInt()
    val row = floor((pieceTopY - boardOriginY) / cellSizePx + 0.5f).toInt()
    if (!canPlacePieceAt(board, pieceCells, row, col)) return null
    return BlockFillDragAnchor(row, col)
}
