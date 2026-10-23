package com.quietgrid.app.games.arrowescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.buildCellOwnerMap
import com.quietgrid.engine.arrowescape.isActiveCell
import com.quietgrid.engine.arrowescape.isPieceRemovable
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.arrowescape.toPiece
import kotlin.math.roundToInt
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

data class ArrowEscapeViewport(val top: Int, val left: Int, val rows: Int, val cols: Int) {
    val aspectRatio: Float get() = cols.toFloat() / rows
}

fun arrowEscapeViewport(puzzle: ArrowEscapePuzzleEntry): ArrowEscapeViewport {
    val used = puzzle.pieces.flatMap { piece -> piece.toPiece().cells.map { it.row to it.col } } +
        (0 until puzzle.rows).flatMap { row -> (0 until puzzle.cols).filter { puzzle.isActiveCell(row, it) }.map { row to it } }
    if (used.isEmpty()) return ArrowEscapeViewport(0, 0, puzzle.rows, puzzle.cols)
    val top = used.minOf { it.first }
    val left = used.minOf { it.second }
    return ArrowEscapeViewport(top, left, used.maxOf { it.first } - top + 1, used.maxOf { it.second } - left + 1)
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
    pieces: List<ArrowEscapePiece> = puzzle.pieces.map { it.toPiece() },
    ownerLookup: Map<Pair<Int, Int>, Int> = buildCellOwnerMap(pieces),
): Int? {
    val inRange = pieces.indices
        .filter { it !in removedIndices }
        .map { index -> index to pieces[index].cells.minOf { distanceToCell(tapRow, tapCol, it.row, it.col) } }
        .filter { (_, distance) -> distance <= toleranceCells }
    if (inRange.isEmpty()) return null
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
    val newHint = hintIndex != session.selectedIndex
    return session.copy(selectedIndex = hintIndex, hintsUsed = session.hintsUsed + if (newHint) 1 else 0)
}

private const val ARROW_ESCAPE_TIME_POINTS = 30_000.0
private const val ARROW_ESCAPE_MISTAKE_POINTS = 12_500.0
private const val ARROW_ESCAPE_HINT_POINTS = 7_500.0
private const val ARROW_ESCAPE_MISTAKE_WEIGHT = 4.0
private const val ARROW_ESCAPE_HINT_WEIGHT = 2.0

private val ARROW_ESCAPE_PAR_SECONDS_PER_ARROW = mapOf(
    Difficulty.EASY to 1.5,
    Difficulty.MEDIUM to 2.0,
    Difficulty.HARD to 2.5,
    Difficulty.EXPERT to 3.0,
)

fun arrowEscapeScore(difficulty: Difficulty, arrowCount: Int, elapsedSeconds: Int, mistakes: Int, hintsUsed: Int): Int {
    val arrows = maxOf(1, arrowCount).toDouble()
    val parSeconds = arrows * ARROW_ESCAPE_PAR_SECONDS_PER_ARROW.getValue(difficulty)
    val timePoints = ARROW_ESCAPE_TIME_POINTS * minOf(1.0, parSeconds / maxOf(1, elapsedSeconds))
    val mistakePoints = ARROW_ESCAPE_MISTAKE_POINTS * maxOf(0.0, 1 - ARROW_ESCAPE_MISTAKE_WEIGHT * mistakes / arrows)
    val hintPoints = ARROW_ESCAPE_HINT_POINTS * maxOf(0.0, 1 - ARROW_ESCAPE_HINT_WEIGHT * hintsUsed / arrows)
    return (timePoints + mistakePoints + hintPoints).roundToInt()
}
