package com.quietgrid.engine.sudoku

import com.quietgrid.engine.sudoku.techniques.BoxLineReductionTechnique
import com.quietgrid.engine.sudoku.techniques.ChainsTechnique
import com.quietgrid.engine.sudoku.techniques.ColoringTechnique
import com.quietgrid.engine.sudoku.techniques.HiddenPairTechnique
import com.quietgrid.engine.sudoku.techniques.HiddenSingleTechnique
import com.quietgrid.engine.sudoku.techniques.NakedPairTechnique
import com.quietgrid.engine.sudoku.techniques.NakedSingleTechnique
import com.quietgrid.engine.sudoku.techniques.PointingPairTripleTechnique
import com.quietgrid.engine.sudoku.techniques.SwordfishTechnique
import com.quietgrid.engine.sudoku.techniques.XWingTechnique
import com.quietgrid.engine.sudoku.techniques.XyWingTechnique
import com.quietgrid.engine.sudoku.techniques.XyzWingTechnique

val orderedTechniqueDispatchers: List<SudokuTechniqueDispatcher> = listOf(
    NakedSingleTechnique, HiddenSingleTechnique, NakedPairTechnique, HiddenPairTechnique,
    PointingPairTripleTechnique, BoxLineReductionTechnique, XWingTechnique, SwordfishTechnique,
    XyWingTechnique, XyzWingTechnique, ColoringTechnique, ChainsTechnique,
)

private val TECHNIQUE_BASE_SCORES: Map<SudokuTechnique, Int> = mapOf(
    SudokuTechnique.NAKED_SINGLE to 2, SudokuTechnique.HIDDEN_SINGLE to 3, SudokuTechnique.NAKED_PAIR to 6,
    SudokuTechnique.HIDDEN_PAIR to 9, SudokuTechnique.POINTING_PAIR_TRIPLE to 10, SudokuTechnique.BOX_LINE_REDUCTION to 10,
    SudokuTechnique.X_WING to 17, SudokuTechnique.SWORDFISH to 24, SudokuTechnique.XY_WING to 21,
    SudokuTechnique.XYZ_WING to 28, SudokuTechnique.COLORING to 27, SudokuTechnique.CHAINS to 30,
)

private fun moveScore(move: SudokuCanonicalMove): Int = TECHNIQUE_BASE_SCORES.getValue(move.technique) + move.complexity

private fun applyMove(state: SudokuBitmaskState, move: SudokuCanonicalMove) {
    when (move) {
        is SudokuPlacementMove -> placeDigit(state, getCellIndex(move.targetRow, move.targetCol), move.digit)
        is SudokuCandidateEliminationMove -> move.eliminations.forEach { (row, col, digit) -> eliminateCandidate(state, getCellIndex(row, col), digit) }
    }
}

private fun findNextMoveInState(state: SudokuBitmaskState, allowedTechniques: List<SudokuTechnique>): SudokuCanonicalMove? {
    val allowedSet = allowedTechniques.toSet()
    var best: SudokuCanonicalMove? = null
    var bestScore = Int.MAX_VALUE

    for (dispatcher in orderedTechniqueDispatchers) {
        if (dispatcher.technique !in allowedSet) continue
        if (TECHNIQUE_BASE_SCORES.getValue(dispatcher.technique) >= bestScore) continue

        val move = dispatcher.findMove(state) ?: continue
        val score = moveScore(move)
        if (score < bestScore) {
            best = move
            bestScore = score
        }
    }
    return best
}

data class SudokuSolveTrace(val solved: Boolean, val blocked: Boolean, val moves: List<SudokuCanonicalMove>)

fun traceHumanSolve(board: SudokuGrid, allowedTechniques: List<SudokuTechnique> = SudokuTechnique.entries): SudokuSolveTrace {
    val state = createBitmaskStateFromBoard(board)
    val moves = mutableListOf<SudokuCanonicalMove>()
    val seenStates = mutableSetOf<String>()

    while (!isSolved(state)) {
        val stateKey = encodeBitmaskState(state)
        if (stateKey in seenStates) return SudokuSolveTrace(solved = false, blocked = true, moves)
        seenStates.add(stateKey)

        val move = findNextMoveInState(state, allowedTechniques) ?: return SudokuSolveTrace(solved = false, blocked = true, moves)
        applyMove(state, move)
        moves.add(move)
        if (moves.size > 512) return SudokuSolveTrace(solved = false, blocked = true, moves)
    }
    return SudokuSolveTrace(solved = true, blocked = false, moves)
}

fun findNextMove(board: SudokuGrid, allowedTechniques: List<SudokuTechnique> = SudokuTechnique.entries): SudokuCanonicalMove? =
    findNextMoveInState(createBitmaskStateFromBoard(board), allowedTechniques)
