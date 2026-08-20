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

fun placePieceAt(board: BlockFillBoard, cells: List<Pair<Int, Int>>, anchorRow: Int, anchorCol: Int, family: BlockFillShapeFamily): BlockFillBoard {
    val next = board.map { it.toMutableList() }
    for ((dr, dc) in cells) next[anchorRow + dr][anchorCol + dc] = family
    return next
}

fun clearFullLines(board: BlockFillBoard): Pair<BlockFillBoard, Int> {
    val fullRows = (0 until BLOCKFILL_BOARD_SIZE).filter { row -> board[row].all { it != null } }.toSet()
    val fullCols = (0 until BLOCKFILL_BOARD_SIZE).filter { col -> board.all { it[col] != null } }.toSet()

    if (fullRows.isEmpty() && fullCols.isEmpty()) return board to 0

    val next = board.mapIndexed { r, row ->
        row.mapIndexed { c, cell -> if (r in fullRows || c in fullCols) null else cell }
    }
    return next to (fullRows.size + fullCols.size)
}

fun previewClearedCells(board: BlockFillBoard, cells: List<Pair<Int, Int>>, anchorRow: Int, anchorCol: Int, family: BlockFillShapeFamily): List<Pair<Int, Int>> {
    if (!canPlacePieceAt(board, cells, anchorRow, anchorCol)) return emptyList()

    val placed = placePieceAt(board, cells, anchorRow, anchorCol, family)
    val (afterClear, linesCleared) = clearFullLines(placed)
    if (linesCleared == 0) return emptyList()

    val result = mutableListOf<Pair<Int, Int>>()
    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
            if (placed[row][col] != null && afterClear[row][col] == null) result.add(row to col)
        }
    }
    return result
}

fun countFilledCells(board: BlockFillBoard): Int = board.sumOf { row -> row.count { it != null } }

fun isBoardEmpty(board: BlockFillBoard): Boolean = countFilledCells(board) == 0
