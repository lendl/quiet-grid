package com.quietgrid.engine.circuit

data class CircuitSolveStep(val technique: CircuitTechnique, val cell: Int, val chainDepth: Int, val chainLength: Int = 0)

data class CircuitSolveResult(val solved: Boolean, val steps: List<CircuitSolveStep>, val masks: IntArray?)

fun nextCircuitDeduction(state: CircuitSolverState, maxChainDepth: Int = CIRCUIT_MAX_CHAIN_DEPTH): CircuitDeduction? =
    findCircuitEdgeMatch(state)
        ?: findCircuitNoLoop(state)
        ?: findCircuitDeadEnd(state)
        ?: findCircuitContradiction(state, maxChainDepth)

fun solveCircuit(board: CircuitBoard, shapes: IntArray, maxChainDepth: Int = CIRCUIT_MAX_CHAIN_DEPTH): CircuitSolveResult =
    solveCircuitFrom(CircuitSolverState.fromShapes(board, shapes), maxChainDepth)

fun solveCircuitFrom(state: CircuitSolverState, maxChainDepth: Int = CIRCUIT_MAX_CHAIN_DEPTH): CircuitSolveResult {
    val steps = mutableListOf<CircuitSolveStep>()
    while (!state.isSolved()) {
        if (hasCircuitContradiction(state)) return CircuitSolveResult(false, steps, null)
        val deduction = nextCircuitDeduction(state, maxChainDepth) ?: return CircuitSolveResult(false, steps, null)
        val chainLength = if (deduction.technique == CircuitTechnique.CONTRADICTION) circuitWhatIfChainLength(state, deduction) else 0
        state.apply(deduction)
        steps += CircuitSolveStep(deduction.technique, deduction.cell, deduction.chainDepth, chainLength)
    }
    val masks = state.values()
    if (state.hasEmptyDomain() || !isCircuitSolved(state.board, masks)) return CircuitSolveResult(false, steps, null)
    return CircuitSolveResult(true, steps, masks)
}

fun countCircuitSolutions(board: CircuitBoard, shapes: IntArray, cap: Int = 2): Int {
    var found = 0

    fun search(state: CircuitSolverState) {
        if (found >= cap) return
        while (true) {
            if (hasCircuitContradiction(state)) return
            val forced = findCircuitEdgeMatch(state) ?: break
            state.apply(forced)
        }
        val open = (0 until board.size).firstOrNull { !state.isDecided(it) }
        if (open == null) {
            if (isCircuitSolved(board, state.values())) found++
            return
        }
        for (mask in state.options(open)) {
            val branch = state.copy()
            branch.fix(open, mask)
            search(branch)
            if (found >= cap) return
        }
    }

    search(CircuitSolverState.fromShapes(board, shapes))
    return found
}

fun circuitShapes(entry: CircuitPuzzleEntry): IntArray = entry.solution.toIntArray()
