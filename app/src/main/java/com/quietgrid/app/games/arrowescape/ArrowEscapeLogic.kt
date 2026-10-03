package com.quietgrid.app.games.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.isPieceRemovable
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.arrowescape.toPiece
import kotlin.math.sqrt

data class ArrowEscapeAttemptResult(val session: ArrowEscapeSession, val removed: Boolean)

fun applyArrowEscapeAttempt(session: ArrowEscapeSession, pieceIndex: Int): ArrowEscapeAttemptResult? {
    if (session.status != ArrowEscapeStatus.PLAYING) return null
    if (pieceIndex in session.removedIndices) return null

    val pieces = session.puzzle.pieces.map { it.toPiece() }
    val ownerLookup = buildCellOwnerMap(pieces)
    val removable = isPieceRemovable(pieceIndex, pieces, session.removedIndices, ownerLookup, session.puzzle.rows, session.puzzle.cols)

    if (!removable) {
        val lives = session.lives - 1
        val status = if (lives <= 0) ArrowEscapeStatus.LOST else ArrowEscapeStatus.PLAYING
        return ArrowEscapeAttemptResult(session.copy(lives = lives, selectedIndex = pieceIndex, status = status), removed = false)
    }

    val removedIndices = session.removedIndices + pieceIndex
    val status = if (removedIndices.size == pieces.size) ArrowEscapeStatus.WON else ArrowEscapeStatus.PLAYING
    return ArrowEscapeAttemptResult(session.copy(removedIndices = removedIndices, selectedIndex = null, status = status), removed = true)
}

private fun distanceToCell(tapRow: Float, tapCol: Float, row: Int, col: Int): Float {
    val dRow = maxOf(row - tapRow, 0f, tapRow - (row + 1))
    val dCol = maxOf(col - tapCol, 0f, tapCol - (col + 1))
    return sqrt(dRow * dRow + dCol * dCol)
}

fun resolveArrowEscapeTap(
    puzzle: ArrowEscapePuzzleEntry,
    removedIndices: Set<Int>,
    tapRow: Float,
    tapCol: Float,
    toleranceCells: Float,
): Int? {
    val pieces = puzzle.pieces.map { it.toPiece() }
    val inRange = pieces.indices
        .filter { it !in removedIndices }
        .map { index -> index to pieces[index].cells.minOf { distanceToCell(tapRow, tapCol, it.row, it.col) } }
        .filter { (_, distance) -> distance <= toleranceCells }
    if (inRange.isEmpty()) return null
    val ownerLookup = buildCellOwnerMap(pieces)
    val removable = inRange.filter { (index, _) ->
        isPieceRemovable(index, pieces, removedIndices, ownerLookup, puzzle.rows, puzzle.cols)
    }
    return removable.ifEmpty { inRange }.minBy { (_, distance) -> distance }.first
}

fun applyArrowEscapeHint(session: ArrowEscapeSession): ArrowEscapeSession? {
    if (session.status != ArrowEscapeStatus.PLAYING) return null
    val pieces = session.puzzle.pieces.map { it.toPiece() }
    val result = solveArrowEscape(pieces, session.puzzle.rows, session.puzzle.cols, alreadyRemoved = session.removedIndices)
    val hintIndex = result.moves.firstOrNull()?.pieceIndex ?: return null
    return session.copy(selectedIndex = hintIndex)
}
