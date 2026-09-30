package com.quietgrid.app.games.blockfill

fun createEmptyBoard(): BlockFillBoard =
    List(BLOCKFILL_BOARD_SIZE) { List(BLOCKFILL_BOARD_SIZE) { null } }

fun canPlacePieceAt(board: BlockFillBoard, cells: List<Pair<Int, Int>>, anchorRow: Int, anchorCol: Int): Boolean =
    cells.all { (dr, dc) ->
        val r = anchorRow + dr
        val c = anchorCol + dc
        r in 0 until BLOCKFILL_BOARD_SIZE && c in 0 until BLOCKFILL_BOARD_SIZE && board[r][c] == null
    }

fun findValidPlacements(board: BlockFillBoard, cells: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    val placements = mutableListOf<Pair<Int, Int>>()
    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
            if (canPlacePieceAt(board, cells, row, col)) placements.add(row to col)
        }
    }
    return placements
}

fun pieceFitsAnywhere(board: BlockFillBoard, cells: List<Pair<Int, Int>>): Boolean {
    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
            if (canPlacePieceAt(board, cells, row, col)) return true
        }
    }
    return false
}

fun placePieceAt(board: BlockFillBoard, cells: List<Pair<Int, Int>>, anchorRow: Int, anchorCol: Int, family: BlockFillShapeFamily): BlockFillBoard {
    val next = board.map { it.toMutableList() }
    for ((dr, dc) in cells) next[anchorRow + dr][anchorCol + dc] = family
    return next
}

data class BlockFillClearedLines(val rows: Set<Int>, val cols: Set<Int>)

private val NO_CLEARED_LINES = BlockFillClearedLines(emptySet(), emptySet())

internal fun fullLines(board: BlockFillBoard): BlockFillClearedLines = BlockFillClearedLines(
    rows = (0 until BLOCKFILL_BOARD_SIZE).filter { row -> board[row].all { it != null } }.toSet(),
    cols = (0 until BLOCKFILL_BOARD_SIZE).filter { col -> board.all { it[col] != null } }.toSet(),
)

fun clearFullLines(board: BlockFillBoard): Pair<BlockFillBoard, Int> {
    val (fullRows, fullCols) = fullLines(board)

    if (fullRows.isEmpty() && fullCols.isEmpty()) return board to 0

    val next = board.mapIndexed { r, row ->
        row.mapIndexed { c, cell -> if (r in fullRows || c in fullCols) null else cell }
    }
    return next to (fullRows.size + fullCols.size)
}

fun previewClearedLines(board: BlockFillBoard, cells: List<Pair<Int, Int>>, anchorRow: Int, anchorCol: Int): BlockFillClearedLines {
    if (!canPlacePieceAt(board, cells, anchorRow, anchorCol)) return NO_CLEARED_LINES
    return fullLines(placePieceAt(board, cells, anchorRow, anchorCol, BlockFillShapeFamily.SINGLE))
}

fun countFilledCells(board: BlockFillBoard): Int = board.sumOf { row -> row.count { it != null } }

fun isBoardEmpty(board: BlockFillBoard): Boolean = countFilledCells(board) == 0
