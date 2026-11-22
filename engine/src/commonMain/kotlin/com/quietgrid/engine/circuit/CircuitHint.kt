package com.quietgrid.engine.circuit

data class CircuitHint(val cell: Int, val mask: Int, val technique: CircuitTechnique?, val isMistake: Boolean)

fun findCircuitHint(puzzle: CircuitPuzzleEntry, current: IntArray, locked: BooleanArray): CircuitHint? {
    val board = puzzle.toBoard()
    val solution = puzzle.solution
    val mistake = solution.indices.firstOrNull { locked[it] && current[it] != solution[it] }
    if (mistake != null) return CircuitHint(mistake, solution[mistake], null, isMistake = true)
    if (solution.indices.all { current[it] == solution[it] }) return null

    val fixed = solution.indices.filter { locked[it] }.associateWith { current[it] }
    val state = CircuitSolverState.withFixed(board, solution.toIntArray(), fixed)
    while (!state.isSolved() && !hasCircuitContradiction(state)) {
        val deduction = nextCircuitDeduction(state) ?: break
        state.apply(deduction)
        val cell = deduction.cell
        if (state.isDecided(cell) && !locked[cell] && current[cell] != solution[cell]) {
            return CircuitHint(cell, solution[cell], deduction.technique, isMistake = false)
        }
    }
    val fallback = solution.indices.first { current[it] != solution[it] }
    return CircuitHint(fallback, solution[fallback], null, isMistake = false)
}
