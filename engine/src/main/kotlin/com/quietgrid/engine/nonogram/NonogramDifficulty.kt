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

enum class NonogramLineTier { FREEBIE, SELF_CONTAINED, DEPENDENT, PROBING }

fun nonogramLineSlack(length: Int, clue: List<Int>): Int {
    if (clue == listOf(0)) return 0
    return length - (clue.sum() + (clue.size - 1))
}

fun classifyNonogramLineTier(length: Int, clue: List<Int>): NonogramLineTier {
    if (nonogramLineSlack(length, clue) == 0) return NonogramLineTier.FREEBIE
    val blankLine: List<NonogramCellValue> = List(length) { null }
    val blankAnalysis = analyzeLine(blankLine, clue)
    return if (blankAnalysis != null && blankAnalysis.overlapFillCells.isNotEmpty()) {
        NonogramLineTier.SELF_CONTAINED
    } else {
        NonogramLineTier.DEPENDENT
    }
}

data class NonogramDifficultyMetrics(
    val steps: Int,
    val filledCells: Int,
    val clueSegments: Int,
    val avgPlacementsAtDeduction: Double,
    val maxPlacementsAtDeduction: Int,
    val singleCellStepCount: Int,
    val crossAxisUnlocks: Int,
    val hardestLineTier: NonogramLineTier,
    val hardestTierRepeats: Int,
    val openingLineTier: NonogramLineTier,
    val freebieFillRatio: Double,
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

private fun copyBoard(board: List<MutableList<NonogramCellValue>>): List<MutableList<NonogramCellValue>> =
    board.map { it.toMutableList() }

private fun hasContradiction(board: List<MutableList<NonogramCellValue>>, rowClues: List<List<Int>>, colClues: List<List<Int>>): Boolean {
    for (rowIndex in rowClues.indices) {
        if (analyzeLine(getLine(board, true, rowIndex), rowClues[rowIndex]) == null) return true
    }
    for (colIndex in colClues.indices) {
        if (analyzeLine(getLine(board, false, colIndex), colClues[colIndex]) == null) return true
    }
    return false
}

private fun hypothesisContradicts(
    board: List<MutableList<NonogramCellValue>>,
    rowClues: List<List<Int>>,
    colClues: List<List<Int>>,
    row: Int,
    col: Int,
    value: Int,
): Boolean {
    val trial = copyBoard(board)
    trial[row][col] = value
    if (hasContradiction(trial, rowClues, colClues)) return true
    while (true) {
        val step = getCanonicalStep(trial, rowClues, colClues) ?: return false
        step.targetCells.forEach { (r, c, v) -> trial[r][c] = v }
        if (hasContradiction(trial, rowClues, colClues)) return true
    }
}

private fun getProbingStep(
    board: List<MutableList<NonogramCellValue>>,
    rowClues: List<List<Int>>,
    colClues: List<List<Int>>,
): CanonicalStep? {
    for (row in rowClues.indices) {
        for (col in colClues.indices) {
            if (board[row][col] != null) continue
            val zeroContradicts = hypothesisContradicts(board, rowClues, colClues, row, col, 0)
            val oneContradicts = hypothesisContradicts(board, rowClues, colClues, row, col, 1)
            if (zeroContradicts && !oneContradicts) return CanonicalStep(listOf(Triple(row, col, 1)), 1, true)
            if (oneContradicts && !zeroContradicts) return CanonicalStep(listOf(Triple(row, col, 0)), 1, true)
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
    var freebieFilledCells = 0
    val safetyLimit = max(8, rows * cols * 2)

    val rowTiers = rowClues.map { classifyNonogramLineTier(cols, it) }
    val colTiers = colClues.map { classifyNonogramLineTier(rows, it) }
    val tierPerStep = mutableListOf<NonogramLineTier>()

    fun allLineAnalyses(): Pair<List<NonogramLineAnalysis?>, List<NonogramLineAnalysis?>> {
        val rows = rowClues.indices.map { analyzeLine(getLine(board, true, it), rowClues[it]) }
        val cols = colClues.indices.map { analyzeLine(getLine(board, false, it), colClues[it]) }
        return rows to cols
    }

    while (!boardIsSolved(board, solution) && steps < safetyLimit) {
        val (beforeRows, beforeCols) = allLineAnalyses()
        var step = getCanonicalStep(board, rowClues, colClues)
        var isProbingStep = false
        if (step == null) {
            step = getProbingStep(board, rowClues, colClues) ?: return null
            isProbingStep = true
        }

        totalPlacements += step.placementCount
        maxPlacementsAtDeduction = max(maxPlacementsAtDeduction, step.placementCount)
        val isOverlapFillSingleCell = step.targetCells.size == 1 && step.targetCells[0].third == 1
        if (isOverlapFillSingleCell) singleCellStepCount += 1

        val (firstRow, firstCol, _) = step.targetCells[0]
        val stepTier = if (isProbingStep) NonogramLineTier.PROBING else if (step.isRow) rowTiers[firstRow] else colTiers[firstCol]
        tierPerStep.add(stepTier)
        if (stepTier == NonogramLineTier.FREEBIE) {
            freebieFilledCells += step.targetCells.count { (_, _, v) -> v == 1 }
        }

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

    val hardestLineTier = tierPerStep.maxByOrNull { it.ordinal } ?: NonogramLineTier.FREEBIE
    val hardestTierRepeats = tierPerStep.count { it == hardestLineTier }
    val openingLineTier = tierPerStep.firstOrNull() ?: NonogramLineTier.FREEBIE
    val filledCells = solution.sumOf { row -> row.count { it } }

    return NonogramDifficultyMetrics(
        steps = steps,
        filledCells = filledCells,
        clueSegments = countClueSegments(rowClues) + countClueSegments(colClues),
        avgPlacementsAtDeduction = if (steps > 0) totalPlacements.toDouble() / steps else 0.0,
        maxPlacementsAtDeduction = maxPlacementsAtDeduction,
        singleCellStepCount = singleCellStepCount,
        crossAxisUnlocks = crossAxisUnlocks,
        hardestLineTier = hardestLineTier,
        hardestTierRepeats = hardestTierRepeats,
        openingLineTier = openingLineTier,
        freebieFillRatio = if (filledCells > 0) freebieFilledCells.toDouble() / filledCells else 0.0,
    )
}

fun computeNonogramScore(metrics: NonogramDifficultyMetrics): Int {
    val placementBonus = metrics.maxPlacementsAtDeduction / 5
    val incrementalBonus = metrics.singleCellStepCount
    val chainBonus = metrics.crossAxisUnlocks / 4
    return metrics.steps + placementBonus + incrementalBonus + chainBonus
}

private const val SELF_CONTAINED_HARD_REPEAT_FLOOR = 8
private const val DEPENDENT_HARD_REPEAT_FLOOR = 3
private const val DEPENDENT_HARD_CROSS_AXIS_FLOOR = 4
private const val DEPENDENT_EXPERT_REPEAT_FLOOR = 17
private const val FREEBIE_EASY_CELL_CEILING = 50
private const val FREEBIE_FILL_RATIO_CEILING_MEDIUM = 0.80
private const val FREEBIE_FILL_RATIO_CEILING_HARD = 0.55
private const val FREEBIE_FILL_RATIO_CEILING_EXPERT = 0.45

private fun openingTierCeilingFor(difficulty: Difficulty): NonogramLineTier? = when (difficulty) {
    Difficulty.EASY -> NonogramLineTier.FREEBIE
    Difficulty.MEDIUM -> NonogramLineTier.SELF_CONTAINED
    Difficulty.HARD -> NonogramLineTier.DEPENDENT
    Difficulty.EXPERT -> null
}

private fun freebieFillRatioCeilingFor(difficulty: Difficulty): Double? = when (difficulty) {
    Difficulty.EASY -> null
    Difficulty.MEDIUM -> FREEBIE_FILL_RATIO_CEILING_MEDIUM
    Difficulty.HARD -> FREEBIE_FILL_RATIO_CEILING_HARD
    Difficulty.EXPERT -> FREEBIE_FILL_RATIO_CEILING_EXPERT
}

fun classifyNonogramDifficulty(rows: Int, cols: Int, metrics: NonogramDifficultyMetrics): Difficulty {
    val shortSide = min(rows, cols)

    var difficulty = when (metrics.hardestLineTier) {
        NonogramLineTier.FREEBIE -> if (rows * cols <= FREEBIE_EASY_CELL_CEILING) Difficulty.EASY else Difficulty.MEDIUM
        NonogramLineTier.SELF_CONTAINED ->
            if (metrics.hardestTierRepeats >= SELF_CONTAINED_HARD_REPEAT_FLOOR) Difficulty.HARD else Difficulty.MEDIUM
        NonogramLineTier.DEPENDENT -> when {
            metrics.hardestTierRepeats >= DEPENDENT_EXPERT_REPEAT_FLOOR && shortSide >= 10 -> Difficulty.EXPERT
            metrics.hardestTierRepeats >= DEPENDENT_HARD_REPEAT_FLOOR && metrics.crossAxisUnlocks >= DEPENDENT_HARD_CROSS_AXIS_FLOOR -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
        NonogramLineTier.PROBING -> Difficulty.EXPERT
    }

    while (true) {
        val ceiling = openingTierCeilingFor(difficulty) ?: break
        if (metrics.openingLineTier.ordinal <= ceiling.ordinal) break
        difficulty = Difficulty.entries[difficulty.ordinal + 1]
    }

    while (difficulty.ordinal > 0) {
        val ceiling = freebieFillRatioCeilingFor(difficulty) ?: break
        if (metrics.freebieFillRatio <= ceiling) break
        difficulty = Difficulty.entries[difficulty.ordinal - 1]
    }

    return difficulty
}

private const val PROBING_EXTREME_REPEAT_FLOOR = 4

fun isExtremeNonogramPuzzle(metrics: NonogramDifficultyMetrics): Boolean =
    metrics.hardestLineTier == NonogramLineTier.PROBING && metrics.hardestTierRepeats >= PROBING_EXTREME_REPEAT_FLOOR
