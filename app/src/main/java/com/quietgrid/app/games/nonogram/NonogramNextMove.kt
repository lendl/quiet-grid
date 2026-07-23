package com.quietgrid.app.games.nonogram

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

private data class LinePlacement(val starts: List<Int>)

private data class LineAnalysis(
    val placements: List<LinePlacement>,
    val overlapFillCells: List<Int>,
    val forcedEmptyCells: List<Int>,
    val isComplete: Boolean,
)

private data class LineCheck(
    val orientation: String, // "row" | "col"
    val index: Int,
    val clues: List<Int>,
    val cells: List<NonogramCellValue>,
    val analysis: LineAnalysis?,
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

private fun rangeHasFilledCell(line: List<NonogramCellValue>, start: Int, endExclusive: Int): Boolean {
    for (index in start until endExclusive) if (line[index] == 1) return true
    return false
}

private fun canPlaceBlockAt(line: List<NonogramCellValue>, start: Int, length: Int): Boolean {
    val end = start + length - 1
    if (end >= line.size) return false
    for (index in start..end) if (line[index] == 0) return false
    if (start > 0 && line[start - 1] == 1) return false
    if (end < line.size - 1 && line[end + 1] == 1) return false
    return true
}

private fun placementCoversAllFilledCells(line: List<NonogramCellValue>, starts: List<Int>, clues: List<Int>): Boolean {
    val intervals = starts.mapIndexed { clueIndex, start -> start to (start + clues[clueIndex] - 1) }
    return line.indices.all { cellIndex ->
        if (line[cellIndex] != 1) true else intervals.any { (start, end) -> cellIndex in start..end }
    }
}

private fun getMinimumRemainingLengths(clues: List<Int>): List<Int> {
    val result = IntArray(clues.size)
    var remaining = 0
    for (index in clues.indices.reversed()) {
        remaining += clues[index]
        result[index] = remaining + (clues.size - index - 1)
    }
    return result.toList()
}

private class StackFrame(val clueIndex: Int, var nextStart: Int)

private fun enumerateLinePlacements(line: List<NonogramCellValue>, clues: List<Int>): List<LinePlacement> {
    if (clues.isEmpty()) {
        return if (line.any { it == 1 }) emptyList() else listOf(LinePlacement(emptyList()))
    }

    val minimumRemainingLengths = getMinimumRemainingLengths(clues)
    if (minimumRemainingLengths[0] > line.size) return emptyList()

    val placements = mutableListOf<LinePlacement>()
    val starts = IntArray(clues.size)
    val stack = mutableListOf(StackFrame(0, 0))

    while (stack.isNotEmpty()) {
        val frame = stack.last()
        val clueIndex = frame.clueIndex
        val clueLength = clues[clueIndex]
        val latestStart = line.size - minimumRemainingLengths[clueIndex]
        var candidateStart = frame.nextStart
        var advanced = false

        while (candidateStart <= latestStart) {
            if (!canPlaceBlockAt(line, candidateStart, clueLength)) {
                candidateStart += 1
                continue
            }

            if (clueIndex == 0) {
                if (rangeHasFilledCell(line, 0, candidateStart)) {
                    candidateStart += 1
                    continue
                }
            } else {
                val previousEnd = starts[clueIndex - 1] + clues[clueIndex - 1] - 1
                val gapStart = previousEnd + 1
                if (candidateStart < gapStart + 1) {
                    candidateStart += 1
                    continue
                }
                if (rangeHasFilledCell(line, gapStart, candidateStart)) {
                    candidateStart += 1
                    continue
                }
            }

            starts[clueIndex] = candidateStart
            frame.nextStart = candidateStart + 1
            advanced = true

            if (clueIndex == clues.size - 1) {
                if (placementCoversAllFilledCells(line, starts.toList(), clues)) {
                    placements.add(LinePlacement(starts.toList()))
                }
            } else {
                stack.add(StackFrame(clueIndex + 1, candidateStart + clueLength + 1))
            }
            break
        }

        if (!advanced) stack.removeAt(stack.size - 1)
    }

    return placements
}

private fun analyzeLine(line: List<NonogramCellValue>, clues: List<Int>): LineAnalysis? {
    val placements = enumerateLinePlacements(line, clues)
    if (placements.isEmpty()) return null

    val coverage = IntArray(line.size)
    for (placement in placements) {
        placement.starts.forEachIndexed { clueIndex, start ->
            val end = start + clues[clueIndex] - 1
            for (index in start..end) coverage[index] += 1
        }
    }

    val overlapFillCells = mutableListOf<Int>()
    val forcedEmptyCells = mutableListOf<Int>()
    line.forEachIndexed { index, cell ->
        if (cell != null) return@forEachIndexed
        when (coverage[index]) {
            placements.size -> overlapFillCells.add(index)
            0 -> forcedEmptyCells.add(index)
        }
    }

    return LineAnalysis(placements, overlapFillCells, forcedEmptyCells, isNonogramLineComplete(line, clues))
}

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
