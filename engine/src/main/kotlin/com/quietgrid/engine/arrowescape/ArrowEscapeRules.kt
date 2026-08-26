// engine/src/main/kotlin/com/quietgrid/engine/arrowescape/ArrowEscapeRules.kt
package com.quietgrid.engine.arrowescape

fun isPieceRemovable(
    pieceIndex: Int,
    pieces: List<ArrowEscapePiece>,
    removedIndices: Set<Int>,
    ownerLookup: Map<Pair<Int, Int>, Int>,
    rows: Int,
    cols: Int,
): Boolean {
    val piece = pieces[pieceIndex]
    val head = piece.cells.last()
    val corridor = computeCorridor(head.row, head.col, piece.headDirection, rows, cols)
    return corridor.all { cell ->
        val ownerIndex = ownerLookup[cell.row to cell.col]
        ownerIndex == null || ownerIndex == pieceIndex || ownerIndex in removedIndices
    }
}

fun findNextRemovablePiece(pieces: List<ArrowEscapePiece>, removedIndices: Set<Int>, rows: Int, cols: Int): Int? {
    val ownerLookup = buildCellOwnerMap(pieces)
    return pieces.indices.firstOrNull { index ->
        index !in removedIndices && isPieceRemovable(index, pieces, removedIndices, ownerLookup, rows, cols)
    }
}
