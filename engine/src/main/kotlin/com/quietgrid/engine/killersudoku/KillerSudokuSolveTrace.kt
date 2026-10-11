package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuCanonicalMove
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.SudokuTechniqueDispatcher
import com.quietgrid.engine.sudoku.cloneBitmaskState
import com.quietgrid.engine.sudoku.encodeBitmaskState
import com.quietgrid.engine.sudoku.getCellIndex
import com.quietgrid.engine.sudoku.isSolved
import com.quietgrid.engine.sudoku.orderedTechniqueDispatchers

interface KillerTechniqueDispatcher {
    val technique: KillerTechnique
    fun findMove(state: KillerSolverState): KillerMove?
}

private fun SudokuCanonicalMove.toKillerMove(): KillerMove = when (this) {
    is SudokuPlacementMove -> KillerMove(
        technique = KillerTechnique.fromClassic(technique),
        complexity = complexity,
        placementCell = getCellIndex(targetRow, targetCol),
        placementDigit = digit,
        eliminations = emptyList(),
        evidenceCells = evidenceCells.map { getCellIndex(it.row, it.col) },
        cages = emptyList(),
        houses = houses,
    )
    is SudokuCandidateEliminationMove -> KillerMove(
        technique = KillerTechnique.fromClassic(technique),
        complexity = complexity,
        placementCell = null,
        placementDigit = null,
        eliminations = eliminations.map { (row, col, digit) -> getCellIndex(row, col) to digit },
        evidenceCells = evidenceCells.map { getCellIndex(it.row, it.col) },
        cages = emptyList(),
        houses = houses,
    )
}

private class ClassicTechnique(private val dispatcher: SudokuTechniqueDispatcher) : KillerTechniqueDispatcher {
    override val technique = KillerTechnique.fromClassic(dispatcher.technique)
    override fun findMove(state: KillerSolverState): KillerMove? = dispatcher.findMove(state.grid)?.toKillerMove()
}

val orderedKillerDispatchers: List<KillerTechniqueDispatcher> = (
    listOf(
        CageLastCellTechnique,
        CageCombinationTechnique,
        RuleOf45Technique(KillerTechnique.INNIE_OUTIE),
        CagePermutationTechnique,
        CageHouseInteractionTechnique,
        RuleOf45Technique(KillerTechnique.RULE_OF_45),
        RuleOf45Technique(KillerTechnique.RULE_OF_45_MULTI_HOUSE),
    ) + orderedTechniqueDispatchers.map { ClassicTechnique(it) }
    ).sortedBy { it.technique.baseScore }

fun findNextKillerMoveInState(state: KillerSolverState, allowed: Set<KillerTechnique> = KillerTechnique.entries.toSet()): KillerMove? {
    for (tier in Difficulty.entries) {
        var best: KillerMove? = null
        for (dispatcher in orderedKillerDispatchers) {
            if (dispatcher.technique.tier != tier || dispatcher.technique !in allowed) continue
            val current = best
            if (current != null && dispatcher.technique.baseScore >= current.score) continue
            val move = dispatcher.findMove(state) ?: continue
            if (current == null || move.score < current.score) best = move
        }
        if (best != null) return best
    }
    return null
}

private val CLASSIC_TECHNIQUES: Set<KillerTechnique> = KillerTechnique.entries.filter { !it.cageBased }.toSet()

data class KillerTraceStep(val move: KillerMove, val filledBefore: Int, val cageRequired: Boolean)

data class KillerSolveTrace(val solved: Boolean, val steps: List<KillerTraceStep>) {
    val moves: List<KillerMove> get() = steps.map { it.move }
}

private const val MAX_TRACE_STEPS = 600

fun traceKillerSolve(
    layout: KillerCageLayout,
    board: IntArray = IntArray(81),
    allowed: Set<KillerTechnique> = KillerTechnique.entries.toSet(),
    measureCageReliance: Boolean = false,
    stopAtFirstPlacement: Boolean = false,
): KillerSolveTrace {
    val state = createKillerSolverState(layout, board)
    val steps = mutableListOf<KillerTraceStep>()
    val seen = mutableSetOf<String>()
    val classicAllowed = allowed intersect CLASSIC_TECHNIQUES

    while (!isSolved(state.grid)) {
        if (!seen.add(encodeBitmaskState(state.grid)) || steps.size >= MAX_TRACE_STEPS) return KillerSolveTrace(false, steps)
        val move = findNextKillerMoveInState(state, allowed) ?: return KillerSolveTrace(false, steps)
        val filled = 81 - state.grid.unresolvedCount
        val cageRequired = measureCageReliance && move.technique.cageBased && findNextKillerMoveInState(state, classicAllowed) == null
        steps += KillerTraceStep(move, filled, cageRequired)
        applyKillerMove(state, move)
        if (stopAtFirstPlacement && move.isPlacement) break
    }
    return KillerSolveTrace(isSolved(state.grid) || stopAtFirstPlacement, steps)
}

data class KillerHint(val cell: Int, val digit: Int, val keyMove: KillerMove, val path: List<KillerMove>)

fun findKillerHint(layout: KillerCageLayout, board: IntArray): KillerHint? {
    val trace = traceKillerSolve(layout, board, stopAtFirstPlacement = true)
    val placement = trace.moves.lastOrNull()?.takeIf { it.isPlacement } ?: return null
    val key = trace.moves.maxWithOrNull(compareBy({ it.technique.tier.ordinal }, { it.technique.baseScore })) ?: placement
    return KillerHint(placement.placementCell!!, placement.placementDigit!!, key, trace.moves)
}

private fun classicCanFinish(source: KillerSolverState, classicAllowed: Set<KillerTechnique>): Boolean {
    val state = KillerSolverState(cloneBitmaskState(source.grid), source.layout)
    var steps = 0
    while (!isSolved(state.grid)) {
        val move = findNextKillerMoveInState(state, classicAllowed) ?: return false
        applyKillerMove(state, move)
        steps += 1
        if (steps >= MAX_TRACE_STEPS) return false
    }
    return true
}

fun measureClassicTakeoverFill(
    layout: KillerCageLayout,
    trace: KillerSolveTrace,
    allowed: Set<KillerTechnique> = KillerTechnique.entries.toSet(),
): Int {
    val classicAllowed = allowed intersect CLASSIC_TECHNIQUES
    fun stateAfter(stepCount: Int): KillerSolverState {
        val state = createKillerSolverState(layout)
        trace.moves.take(stepCount).forEach { applyKillerMove(state, it) }
        return state
    }
    var low = 0
    var high = trace.moves.size
    while (low < high) {
        val mid = (low + high) / 2
        if (classicCanFinish(stateAfter(mid), classicAllowed)) high = mid else low = mid + 1
    }
    return 81 - stateAfter(low).grid.unresolvedCount
}
