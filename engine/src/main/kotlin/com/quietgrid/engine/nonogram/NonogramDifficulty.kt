package com.quietgrid.engine.nonogram

import com.quietgrid.engine.core.Difficulty
import kotlin.math.max
import kotlin.math.min

fun buildNonogramClues(line: List<Boolean>): List<Int> {
    val clues = mutableListOf<Int>()
    var run = 0
    for (cell in line) {
        if (cell) {
            run += 1
        } else if (run > 0) {
            clues.add(run)
            run = 0
        }
    }
    if (run > 0) clues.add(run)
    return if (clues.isNotEmpty()) clues else listOf(0)
}

data class NonogramDifficultyMetrics(
    val steps: Int,
    val filledCells: Int,
    val clueSegments: Int,
    val avgPlacementsAtDeduction: Double,
    val maxPlacementsAtDeduction: Int,
    val singleCellStepCount: Int,
    val crossAxisUnlocks: Int,
)

private data class CanonicalStep(
    val targetCells: List<Triple<Int, Int, Int>>,
    val placementCount: Int,
    val isRow: Boolean,
)

private fun getLine(board: List<MutableList<NonogramCellValue>>, isRow: Boolean, index: Int): List<NonogramCellValue> =
    if (isRow) board[index] else board.map { it[index] }

private fun buildTargets(isRow: Boolean, index: Int, cellIndexes: List<Int>, value: Int): List<Triple<Int, Int, Int>> =
    cellIndexes.map { cellIndex -> if (isRow) Triple(index, cellIndex, value) else Triple(cellIndex, index, value) }

private fun isLineActionable(analysis: NonogramLineAnalysis?): Boolean =
    analysis != null && (analysis.overlapFillCells.isNotEmpty() || analysis.forcedEmptyCells.isNotEmpty())

private fun getCanonicalStep(
    board: List<MutableList<NonogramCellValue>>,
    rowClues: List<List<Int>>,
    colClues: List<List<Int>>,
): CanonicalStep? {
    for (rowIndex in rowClues.indices) {
        val line = getLine(board, isRow = true, rowIndex)
        val analysis = analyzeLine(line, rowClues[rowIndex]) ?: return null
        if (analysis.overlapFillCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(true, rowIndex, analysis.overlapFillCells, 1), analysis.placements.size, true)
        }
        if (analysis.isComplete && analysis.forcedEmptyCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(true, rowIndex, analysis.forcedEmptyCells, 0), analysis.placements.size, true)
        }
        if (analysis.forcedEmptyCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(true, rowIndex, analysis.forcedEmptyCells, 0), analysis.placements.size, true)
        }
    }
    for (colIndex in colClues.indices) {
        val line = getLine(board, isRow = false, colIndex)
        val analysis = analyzeLine(line, colClues[colIndex]) ?: return null
        if (analysis.overlapFillCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(false, colIndex, analysis.overlapFillCells, 1), analysis.placements.size, false)
        }
        if (analysis.isComplete && analysis.forcedEmptyCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(false, colIndex, analysis.forcedEmptyCells, 0), analysis.placements.size, false)
        }
        if (analysis.forcedEmptyCells.isNotEmpty()) {
            return CanonicalStep(buildTargets(false, colIndex, analysis.forcedEmptyCells, 0), analysis.placements.size, false)
        }
    }
    return null
}

private fun boardIsSolved(board: List<List<NonogramCellValue>>, solution: List<List<Boolean>>): Boolean =
    board.indices.all { r -> board[r].indices.all { c -> if (solution[r][c]) board[r][c] == 1 else board[r][c] != 1 } }

private fun countClueSegments(clues: List<List<Int>>): Int = clues.sumOf { line -> line.count { it > 0 } }

fun analyzeNonogramDifficulty(rowClues: List<List<Int>>, colClues: List<List<Int>>, solution: List<List<Boolean>>): NonogramDifficultyMetrics? {
    val rows = rowClues.size
    val cols = colClues.size
    val board = MutableList(rows) { MutableList<NonogramCellValue>(cols) { null } }
    var steps = 0
    var totalPlacements = 0
    var maxPlacementsAtDeduction = 0
    var singleCellStepCount = 0
    var crossAxisUnlocks = 0
    val safetyLimit = max(8, rows * cols * 2)

    fun allLineAnalyses(): Pair<List<NonogramLineAnalysis?>, List<NonogramLineAnalysis?>> {
        val rows = rowClues.indices.map { analyzeLine(getLine(board, true, it), rowClues[it]) }
        val cols = colClues.indices.map { analyzeLine(getLine(board, false, it), colClues[it]) }
        return rows to cols
    }

    while (!boardIsSolved(board, solution) && steps < safetyLimit) {
        val (beforeRows, beforeCols) = allLineAnalyses()
        val step = getCanonicalStep(board, rowClues, colClues) ?: return null

        totalPlacements += step.placementCount
        maxPlacementsAtDeduction = max(maxPlacementsAtDeduction, step.placementCount)
        val isOverlapFillSingleCell = step.targetCells.size == 1 && step.targetCells[0].third == 1
        if (isOverlapFillSingleCell) singleCellStepCount += 1

        step.targetCells.forEach { (r, c, v) -> board[r][c] = v }
        steps += 1

        val (afterRows, afterCols) = allLineAnalyses()
        val beforeOpposite = if (step.isRow) beforeCols else beforeRows
        val afterOpposite = if (step.isRow) afterCols else afterRows
        if (afterOpposite.indices.any { i -> !isLineActionable(beforeOpposite.getOrNull(i)) && isLineActionable(afterOpposite[i]) }) {
            crossAxisUnlocks += 1
        }
    }

    if (!boardIsSolved(board, solution)) return null

    return NonogramDifficultyMetrics(
        steps = steps,
        filledCells = solution.sumOf { row -> row.count { it } },
        clueSegments = countClueSegments(rowClues) + countClueSegments(colClues),
        avgPlacementsAtDeduction = if (steps > 0) totalPlacements.toDouble() / steps else 0.0,
        maxPlacementsAtDeduction = maxPlacementsAtDeduction,
        singleCellStepCount = singleCellStepCount,
        crossAxisUnlocks = crossAxisUnlocks,
    )
}

fun computeNonogramScore(metrics: NonogramDifficultyMetrics): Int {
    val placementBonus = metrics.maxPlacementsAtDeduction / 5
    val incrementalBonus = metrics.singleCellStepCount
    val chainBonus = metrics.crossAxisUnlocks / 4
    return metrics.steps + placementBonus + incrementalBonus + chainBonus
}

fun classifyNonogramDifficulty(rows: Int, cols: Int, metrics: NonogramDifficultyMetrics): Difficulty {
    val score = computeNonogramScore(metrics)
    val shortSide = min(rows, cols)
    val longSide = max(rows, cols)

    var difficulty = when {
        shortSide <= 5 && longSide <= 5 -> when {
            score <= 3 -> Difficulty.EASY
            score <= 5 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
        shortSide <= 5 && longSide <= 10 -> when {
            score <= 4 -> Difficulty.EASY
            score <= 7 -> Difficulty.MEDIUM
            score <= 10 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
        else -> when {
            score <= 6 -> Difficulty.MEDIUM
            score <= 9 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
    }

    if (difficulty == Difficulty.EXPERT && shortSide < 10) difficulty = Difficulty.HARD
    if (difficulty == Difficulty.EASY && longSide > 5) difficulty = Difficulty.MEDIUM
    if (difficulty == Difficulty.EXPERT && metrics.steps < 20) difficulty = Difficulty.HARD
    if (difficulty == Difficulty.HARD && metrics.steps < 15) difficulty = Difficulty.MEDIUM
    if (difficulty == Difficulty.MEDIUM && metrics.steps < 10) difficulty = Difficulty.EASY

    return difficulty
}
