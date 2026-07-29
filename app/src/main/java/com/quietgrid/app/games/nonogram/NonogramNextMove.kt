package com.quietgrid.app.games.nonogram

import com.quietgrid.engine.nonogram.NonogramCellValue
import com.quietgrid.engine.nonogram.NonogramGrid
import com.quietgrid.engine.nonogram.NonogramLineAnalysis
import com.quietgrid.engine.nonogram.analyzeLine
import com.quietgrid.engine.nonogram.isNonogramLineComplete

/**
 * Ports the RN app's Nonogram next-move hint (src/games/nonogram/gameplay/analysis/nextMove.ts,
 * built on gameplay/rules/solver.ts). Unlike Sudoku/Takuzu, Nonogram has one technique, applied
 * uniformly per line: enumerate every valid placement of a line's clue-blocks consistent with
 * its current filled/empty cells; a blank cell covered by every placement must be filled
 * (overlap-fill), one covered by no placement must be empty (forced-empty), and a line whose
 * filled runs already match its clues has its remaining blanks forced empty too (complete-line).
 * A line with zero valid placements means a mistake was made (invalid-board). This is a full,
 * unreduced port — no techniques were skipped.
 */

private data class LineCheck(
    val orientation: String, // "row" | "col"
    val index: Int,
    val clues: List<Int>,
    val cells: List<NonogramCellValue>,
    val analysis: NonogramLineAnalysis?,
)

data class NonogramNextMoveTarget(val row: Int, val col: Int, val value: Int)

sealed interface NonogramNextMoveHint {
    val evidenceCells: List<Pair<Int, Int>>
    val targetCells: List<NonogramNextMoveTarget>
    val lineOrientation: String
    val lineIndex: Int
}

data class NonogramInvalidBoardHint(
    override val evidenceCells: List<Pair<Int, Int>>,
    override val lineOrientation: String,
    override val lineIndex: Int,
) : NonogramNextMoveHint {
    override val targetCells = emptyList<NonogramNextMoveTarget>()
}

enum class NonogramHintKind { OVERLAP_FILL, FORCED_EMPTY, COMPLETE_LINE }

data class NonogramProgressHint(
    val kind: NonogramHintKind,
    val targetCount: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<NonogramNextMoveTarget>,
    override val lineOrientation: String,
    override val lineIndex: Int,
) : NonogramNextMoveHint

private fun getLineCells(board: NonogramGrid, orientation: String, index: Int): List<NonogramCellValue> =
    if (orientation == "row") board[index] else board.map { it[index] }

private fun lineCellsToRefs(orientation: String, index: Int, cellIndexes: List<Int>): List<Pair<Int, Int>> =
    cellIndexes.map { cellIndex -> if (orientation == "row") index to cellIndex else cellIndex to index }

private fun buildLineCheck(board: NonogramGrid, clues: List<Int>, orientation: String, index: Int): LineCheck {
    val cells = getLineCells(board, orientation, index)
    return LineCheck(orientation, index, clues, cells, analyzeLine(cells, clues))
}

private fun buildHintFromLine(line: LineCheck): NonogramNextMoveHint? {
    val analysis = line.analysis ?: return null
    val allCellIndexes = line.cells.indices.toList()
    val evidence = lineCellsToRefs(line.orientation, line.index, allCellIndexes)

    if (analysis.overlapFillCells.isNotEmpty()) {
        val targets = lineCellsToRefs(line.orientation, line.index, analysis.overlapFillCells)
            .map { (r, c) -> NonogramNextMoveTarget(r, c, 1) }
        return NonogramProgressHint(NonogramHintKind.OVERLAP_FILL, analysis.overlapFillCells.size, evidence, targets, line.orientation, line.index)
    }

    if (analysis.isComplete && analysis.forcedEmptyCells.isNotEmpty()) {
        val targets = lineCellsToRefs(line.orientation, line.index, analysis.forcedEmptyCells)
            .map { (r, c) -> NonogramNextMoveTarget(r, c, 0) }
        return NonogramProgressHint(NonogramHintKind.COMPLETE_LINE, analysis.forcedEmptyCells.size, evidence, targets, line.orientation, line.index)
    }

    if (analysis.forcedEmptyCells.isNotEmpty()) {
        val targets = lineCellsToRefs(line.orientation, line.index, analysis.forcedEmptyCells)
            .map { (r, c) -> NonogramNextMoveTarget(r, c, 0) }
        return NonogramProgressHint(NonogramHintKind.FORCED_EMPTY, analysis.forcedEmptyCells.size, evidence, targets, line.orientation, line.index)
    }

    return null
}

fun getNonogramNextMoveHint(puzzle: NonogramPuzzle, board: NonogramGrid): NonogramNextMoveHint? {
    val rows = puzzle.rowClues.mapIndexed { rowIndex, clues -> buildLineCheck(board, clues, "row", rowIndex) }
    val cols = puzzle.colClues.mapIndexed { colIndex, clues -> buildLineCheck(board, clues, "col", colIndex) }
    val invalidLine = (rows + cols).firstOrNull { it.analysis == null }
    if (invalidLine != null) {
        val evidence = lineCellsToRefs(invalidLine.orientation, invalidLine.index, invalidLine.cells.indices.toList())
        return NonogramInvalidBoardHint(evidence, invalidLine.orientation, invalidLine.index)
    }

    for (line in rows + cols) {
        val hint = buildHintFromLine(line)
        if (hint != null) return hint
    }
    return null
}
