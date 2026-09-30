package com.quietgrid.app.games.blockfill

private fun cellBit(row: Int, col: Int): Long = 1L shl (row * BLOCKFILL_BOARD_SIZE + col)

private val ROW_MASKS = LongArray(BLOCKFILL_BOARD_SIZE) { row ->
    (0 until BLOCKFILL_BOARD_SIZE).fold(0L) { mask, col -> mask or cellBit(row, col) }
}

private val COL_MASKS = LongArray(BLOCKFILL_BOARD_SIZE) { col ->
    (0 until BLOCKFILL_BOARD_SIZE).fold(0L) { mask, row -> mask or cellBit(row, col) }
}

fun BlockFillBoard.toMask(): Long {
    var mask = 0L
    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
            if (this[row][col] != null) mask = mask or cellBit(row, col)
        }
    }
    return mask
}

private fun placementMasks(cells: List<Pair<Int, Int>>): LongArray {
    val maxRow = cells.maxOf { it.first }
    val maxCol = cells.maxOf { it.second }
    val masks = mutableListOf<Long>()
    for (anchorRow in 0 until BLOCKFILL_BOARD_SIZE - maxRow) {
        for (anchorCol in 0 until BLOCKFILL_BOARD_SIZE - maxCol) {
            masks.add(cells.fold(0L) { mask, (dr, dc) -> mask or cellBit(anchorRow + dr, anchorCol + dc) })
        }
    }
    return masks.toLongArray()
}

private fun clearFullLinesMask(mask: Long): Long {
    var cleared = 0L
    for (line in ROW_MASKS) if (mask and line == line) cleared = cleared or line
    for (line in COL_MASKS) if (mask and line == line) cleared = cleared or line
    return mask and cleared.inv()
}

private class BoardMaskSearch(private val pieces: List<LongArray>) {
    private val allUsed = (1 shl pieces.size) - 1
    private val memo = Array(1 shl pieces.size) { HashMap<Long, Int>() }

    fun fewestLeft(mask: Long, used: Int): Int {
        if (used == allUsed) return java.lang.Long.bitCount(mask)
        memo[used][mask]?.let { return it }
        var best = Int.MAX_VALUE
        search@ for (index in pieces.indices) {
            if (used and (1 shl index) != 0) continue
            for (placement in pieces[index]) {
                if (placement and mask != 0L) continue
                val left = fewestLeft(clearFullLinesMask(mask or placement), used or (1 shl index))
                if (left < best) {
                    best = left
                    if (best == 0) break@search
                }
            }
        }
        memo[used][mask] = best
        return best
    }
}

private fun fewestLeftOrMax(board: BlockFillBoard, tray: List<BlockFillPiece>): Int =
    BoardMaskSearch(tray.map { placementMasks(it.cells) }).fewestLeft(board.toMask(), 0)

fun canEmptyBoard(board: BlockFillBoard, tray: List<BlockFillPiece>): Boolean = fewestLeftOrMax(board, tray) == 0

fun fewestBlocksLeft(board: BlockFillBoard, tray: List<BlockFillPiece>): Int? =
    fewestLeftOrMax(board, tray).takeIf { it != Int.MAX_VALUE }
