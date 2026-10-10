package com.quietgrid.engine.hashi

data class HashiSolveStep(val technique: HashiTechnique, val chainDepth: Int)

data class HashiSolveResult(val solved: Boolean, val steps: List<HashiSolveStep>, val counts: IntArray?)

fun nextHashiDeduction(state: HashiSolverState, maxChainDepth: Int = HASHI_MAX_CHAIN_DEPTH): HashiDeduction? =
    findHashiCrossing(state)
        ?: findHashiCapacity(state)
        ?: findHashiIsolation(state)
        ?: findHashiContradiction(state, maxChainDepth)

fun solveHashi(board: HashiBoard, maxChainDepth: Int = HASHI_MAX_CHAIN_DEPTH): HashiSolveResult {
    val state = HashiSolverState(board)
    val steps = mutableListOf<HashiSolveStep>()
    while (!state.isSolved()) {
        if (hasHashiContradiction(state)) return HashiSolveResult(false, steps, null)
        val deduction = nextHashiDeduction(state, maxChainDepth) ?: return HashiSolveResult(false, steps, null)
        state.apply(deduction)
        steps += HashiSolveStep(deduction.technique, deduction.chainDepth)
    }
    val counts = state.values()
    if (state.hasEmptyDomain() || !isHashiSolved(board, counts)) return HashiSolveResult(false, steps, null)
    return HashiSolveResult(true, steps, counts)
}

fun countHashiSolutions(board: HashiBoard, cap: Int = 2): Int {
    var found = 0

    fun search(state: HashiSolverState) {
        if (found >= cap) return
        while (true) {
            if (hasHashiContradiction(state)) return
            val forced = findHashiCrossing(state) ?: findHashiCapacity(state) ?: break
            state.apply(forced)
        }
        val open = board.edges.indices.firstOrNull { !state.isDecided(it) }
        if (open == null) {
            if (isHashiSolved(board, state.values())) found++
            return
        }
        for (value in 0..HASHI_MAX_BRIDGES_PER_EDGE) {
            if (!state.allows(open, value)) continue
            val branch = state.copy()
            branch.fix(open, value)
            search(branch)
            if (found >= cap) return
        }
    }

    search(HashiSolverState(board))
    return found
}
