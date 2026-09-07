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
    val freebieLineRatio: Double,
    val maxChainDepth: Int,
    val distinctTechniqueSignatures: Int,
    val duplicateTrickRatio: Double,
    val realStepCount: Int,
    val multiSegmentLineCount: Int,
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

private fun countMultiSegmentLines(clues: List<List<Int>>): Int = clues.count { it.size >= 2 }

private data class CellOrigin(val generation: Int, val setByRowStep: Boolean)

fun analyzeNonogramDifficulty(rowClues: List<List<Int>>, colClues: List<List<Int>>, solution: List<List<Boolean>>): NonogramDifficultyMetrics? {
    val rows = rowClues.size
    val cols = colClues.size
    val board = MutableList(rows) { MutableList<NonogramCellValue>(cols) { null } }
    val cellOrigin = Array(rows) { arrayOfNulls<CellOrigin>(cols) }
    var steps = 0
    var totalPlacements = 0
    var maxPlacementsAtDeduction = 0
    var singleCellStepCount = 0
    var crossAxisUnlocks = 0
    var freebieFilledCells = 0
    var maxChainDepth = 0
    var realStepCount = 0
    val techniqueSignatures = mutableSetOf<Pair<NonogramLineTier, List<Int>>>()
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

        val stepGeneration = if (stepTier == NonogramLineTier.FREEBIE) {
            0
        } else {
            val informingLine = getLine(board, step.isRow, if (step.isRow) firstRow else firstCol)
            val crossAxisGeneration = informingLine.indices
                .filter { informingLine[it] != null }
                .mapNotNull { index -> if (step.isRow) cellOrigin[firstRow][index] else cellOrigin[index][firstCol] }
                // Same-axis-same-line origins are this line's own earlier progress, not
                // external chaining - only a cross-axis origin represents genuinely new
                // information this step depended on.
                .filter { origin -> origin.setByRowStep != step.isRow }
                .maxOfOrNull { origin -> origin.generation }
                ?: 0
            1 + max(0, crossAxisGeneration)
        }
        maxChainDepth = max(maxChainDepth, stepGeneration)
        if (stepTier == NonogramLineTier.SELF_CONTAINED || stepTier == NonogramLineTier.DEPENDENT) {
            realStepCount += 1
            val clue = if (step.isRow) rowClues[firstRow] else colClues[firstCol]
            techniqueSignatures.add(stepTier to clue)
        }

        val stepOrigin = CellOrigin(stepGeneration, step.isRow)
        step.targetCells.forEach { (r, c, v) -> board[r][c] = v; cellOrigin[r][c] = stepOrigin }
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
    val totalLines = rowTiers.size + colTiers.size
    val freebieLineCount = rowTiers.count { it == NonogramLineTier.FREEBIE } + colTiers.count { it == NonogramLineTier.FREEBIE }

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
        freebieLineRatio = if (totalLines > 0) freebieLineCount.toDouble() / totalLines else 0.0,
        maxChainDepth = maxChainDepth,
        distinctTechniqueSignatures = techniqueSignatures.size,
        duplicateTrickRatio = if (realStepCount > 0) 1.0 - techniqueSignatures.size.toDouble() / realStepCount else 0.0,
        realStepCount = realStepCount,
        multiSegmentLineCount = countMultiSegmentLines(rowClues) + countMultiSegmentLines(colClues),
    )
}

fun computeNonogramScore(metrics: NonogramDifficultyMetrics): Int {
    val placementBonus = metrics.maxPlacementsAtDeduction / 5
    val incrementalBonus = metrics.singleCellStepCount
    val chainBonus = metrics.crossAxisUnlocks / 4
    return metrics.steps + placementBonus + incrementalBonus + chainBonus
}

private const val EXPERT_MIN_SHORT_SIDE = 10

// Like chain depth, duplicate-trick ratio rises with board size on its own - more lines means
// more reuse of the same limited pool of clue shapes, independent of whether the puzzle is
// actually repetitive in the bad sense. Calibrated empirically (2000-sample raw generation runs,
// 10x10): even shallow, easy-range candidates (depth<=10) mostly sit at 0.6-0.7, well above the
// flat 0.5 ceiling this replaces - that ceiling was only ever validated against 5x5 examples and
// silently made expert (which additionally needs depth>20) impossible: 0 of 166 real depth>20
// samples ever had ratio<=0.5. 0.75 keeps excluding genuinely degenerate cases (the original
// rectangle-duplication example measured at 0.78) while passing the bulk (160/166) of real
// depth>20 candidates. Interpolated linearly between the two calibrated endpoints (5x5 at
// 10 lines -> 0.5, 10x10 at 20 lines -> 0.75); 10x5's midpoint isn't independently measured.
private fun duplicateTrickRatioCeiling(rows: Int, cols: Int): Double {
    val lines = rows + cols
    return (0.5 + (lines - 10) * 0.025).coerceAtMost(0.8)
}

// Chain depth scales strongly with board size - a 10x10 board naturally needs deeper cross-line
// chains than a 5x5 one just from having more lines to sequentially resolve, independent of how
// "hard" any single deduction is. Anchored empirically (500-sample raw-generation runs per size,
// no difficulty target, just measuring what the shape generator naturally produces):
//   5x5:   depth range 1-10,  median ~4
//   10x5:  depth range 1-18,  median ~7
//   10x10: depth range 4-26,  median ~14
// cellCount/10 approximates the bottom ~10-15% of each real distribution (5x5 -> unit 2, 10x5 ->
// unit 5, 10x10 -> unit 10) and anchors the EASY ceiling. The medium/hard/expert multipliers
// below (1.5x, 2x) are fit against the same three real distributions, not extrapolated blindly -
// an earlier version used 2x/4x, which looked reasonable in isolation but put the expert
// threshold (depthUnit*4 = 40 at 10x10) entirely outside the generator's real reachable range
// (max observed: 26) - expert was silently unreachable. 2x (=20 at 10x10) sits inside the real
// distribution's top ~10%. Signature richness is scaled the same way, off total line count
// instead of cell count, since it's a per-line property.
private fun chainDepthUnit(rows: Int, cols: Int): Int = max(1, (rows * cols) / 10)
private fun signatureUnit(rows: Int, cols: Int): Int = max(2, (rows + cols) / 5)

fun classifyNonogramDifficulty(rows: Int, cols: Int, metrics: NonogramDifficultyMetrics): Difficulty {
    if (metrics.hardestLineTier == NonogramLineTier.PROBING) return Difficulty.EXPERT

    val shortSide = min(rows, cols)
    val depth = metrics.maxChainDepth
    val signatures = metrics.distinctTechniqueSignatures
    val depthUnit = chainDepthUnit(rows, cols)
    val sigUnit = signatureUnit(rows, cols)
    val mediumDepthCeiling = depthUnit + depthUnit / 2
    val hardDepthCeiling = depthUnit * 2

    return when {
        depth <= depthUnit && signatures <= sigUnit -> Difficulty.EASY
        depth <= depthUnit -> Difficulty.MEDIUM
        depth <= mediumDepthCeiling && signatures <= sigUnit * 2 -> Difficulty.MEDIUM
        depth <= mediumDepthCeiling -> Difficulty.HARD
        depth <= hardDepthCeiling || shortSide < EXPERT_MIN_SHORT_SIDE -> Difficulty.HARD
        else -> Difficulty.EXPERT
    }
}

private const val PROBING_EXTREME_REPEAT_FLOOR = 4

fun isExtremeNonogramPuzzle(metrics: NonogramDifficultyMetrics): Boolean =
    metrics.hardestLineTier == NonogramLineTier.PROBING && metrics.hardestTierRepeats >= PROBING_EXTREME_REPEAT_FLOOR

private const val FREEBIE_FILL_RATIO_CEILING = 0.5
private const val MIN_DISTINCT_SIGNATURES_HARD_AND_UP_MULTIPLIER = 3
private fun minMultiSegmentLines(rows: Int, cols: Int): Int = max(3, (rows + cols) / 5)

fun isDegenerateNonogramPuzzle(rows: Int, cols: Int, difficulty: Difficulty, metrics: NonogramDifficultyMetrics): Boolean {
    if (metrics.hardestLineTier == NonogramLineTier.FREEBIE) return true
    if (metrics.duplicateTrickRatio > duplicateTrickRatioCeiling(rows, cols)) return true
    // A solid blob shape (no internal holes/gaps) never produces a clue with more than one
    // number, no matter how deep the chain or how many distinct techniques were needed -
    // every line reduces to "one run, figure out where." Real placement reasoning (where does
    // the gap between segments go) only shows up once some lines actually have 2+ segments.
    if (metrics.multiSegmentLineCount < minMultiSegmentLines(rows, cols)) return true
    // A puzzle can clear both checks above yet still be mostly "read the clue, transcribe it" -
    // if most of what's actually drawn came from lines that were fully forced by their own
    // clue alone, the handful of real steps are often just cleaning up what's left, not solving
    // anything. This is what caught the "three freebie lines fill 80% of the board" case.
    if (metrics.freebieFillRatio > FREEBIE_FILL_RATIO_CEILING) return true
    // Duplicate-trick ratio alone isn't scale-aware: a puzzle can sit at the same ~45% repeat
    // ratio whether it has 5 distinct tricks over 9 steps (thin, samey) or 11 over 20 (rich
    // despite some reuse). Hard/Expert additionally need an absolute floor on real variety,
    // scaled the same way as the classifier's own signature unit (5x5 -> 6, matching the flat
    // value this replaced; bigger boards need proportionally more). Probing is exempt - a
    // contradiction/guess-and-check step is already strong evidence of genuine difficulty on
    // its own, independent of how many line-technique signatures it used.
    if (difficulty.ordinal >= Difficulty.HARD.ordinal &&
        metrics.hardestLineTier != NonogramLineTier.PROBING &&
        metrics.distinctTechniqueSignatures < signatureUnit(rows, cols) * MIN_DISTINCT_SIGNATURES_HARD_AND_UP_MULTIPLIER
    ) {
        return true
    }
    return false
}
