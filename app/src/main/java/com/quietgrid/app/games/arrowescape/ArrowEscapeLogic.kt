package com.quietgrid.app.games.arrowescape

import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.findNextRemovablePiece
import com.quietgrid.engine.arrowescape.isPieceRemovable
import com.quietgrid.engine.arrowescape.toPiece

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

fun applyArrowEscapeHint(session: ArrowEscapeSession): ArrowEscapeSession? {
    if (session.status != ArrowEscapeStatus.PLAYING) return null
    val pieces = session.puzzle.pieces.map { it.toPiece() }
    val hintIndex = findNextRemovablePiece(pieces, session.removedIndices, session.puzzle.rows, session.puzzle.cols) ?: return null
    return session.copy(selectedIndex = hintIndex)
}
