package com.quietgrid.engine.battleship

data class BattleshipSolveResult(val solved: Boolean, val steps: List<BattleshipStep>, val board: BattleshipBoard = emptyList())

fun solveBattleship(
    size: Int,
    rowClues: List<Int>,
    colClues: List<Int>,
    fleet: List<Int>,
    givens: List<Triple<Int, Int, BattleshipCell>> = emptyList(),
): BattleshipSolveResult {
    val state = BattleshipSolverState(size, rowClues.toIntArray(), colClues.toIntArray(), fleet)
    givens.forEach { (row, col, cell) -> state.grid[row][col] = cell }
    if (state.hasContradiction()) return BattleshipSolveResult(solved = false, steps = emptyList(), board = state.gridSnapshot())
    val steps = mutableListOf<BattleshipStep>()
    while (!state.isSolved()) {
        val step = findStructuralWater(state)
            ?: findLineSaturation(state)
            ?: findLineExhaustion(state)
            ?: findFleetElimination(state)
            ?: findFleetLengthMatch(state)
            ?: findProbing(state)
            ?: return BattleshipSolveResult(solved = false, steps = steps, board = state.gridSnapshot())
        applyStep(state, step)
        steps.add(step)
        if (state.hasContradiction()) return BattleshipSolveResult(solved = false, steps = steps, board = state.gridSnapshot())
    }
    return BattleshipSolveResult(solved = true, steps = steps, board = state.gridSnapshot())
}
