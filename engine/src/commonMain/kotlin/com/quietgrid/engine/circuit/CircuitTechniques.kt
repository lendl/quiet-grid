package com.quietgrid.engine.circuit

enum class CircuitTechnique { EDGE_MATCH, NO_LOOP, DEAD_END, CONTRADICTION }

data class CircuitDeduction(
    val technique: CircuitTechnique,
    val cell: Int,
    val keep: Int,
    val chainDepth: Int = 0,
)

fun CircuitSolverState.apply(deduction: CircuitDeduction) {
    restrict(deduction.cell, deduction.keep)
}

private fun CircuitSolverState.edgeMatchKeep(cell: Int): Int {
    var need = 0
    var forbid = 0
    for (direction in CIRCUIT_DIRECTIONS) {
        val other = board.neighbor(cell, direction)
        val opposite = oppositeCircuitDirection(direction)
        when {
            other < 0 -> forbid = forbid or direction
            !canHave(other, opposite) -> forbid = forbid or direction
            mustHave(other, opposite) -> need = need or direction
        }
    }
    var keep = 0
    for (mask in options(cell)) if (mask and need == need && mask and forbid == 0) keep = keep or (1 shl mask)
    return keep
}

fun findCircuitEdgeMatch(state: CircuitSolverState): CircuitDeduction? {
    for (cell in 0 until state.board.size) {
        if (state.domains[cell] == 0) continue
        val keep = state.edgeMatchKeep(cell)
        if (keep != state.domains[cell]) return CircuitDeduction(CircuitTechnique.EDGE_MATCH, cell, keep)
    }
    return null
}

private fun CircuitSolverState.closesLoop(cell: Int, mask: Int, groups: CircuitUnionFind): Boolean {
    val reached = HashSet<Int>()
    reached += groups.find(cell)
    for (direction in CIRCUIT_DIRECTIONS) {
        if (mask and direction == 0) continue
        val other = board.neighbor(cell, direction)
        if (other < 0 || linkForced(cell, direction)) continue
        if (!reached.add(groups.find(other))) return true
    }
    return false
}

fun findCircuitNoLoop(state: CircuitSolverState): CircuitDeduction? {
    val (groups, _) = state.forcedComponents()
    for (cell in 0 until state.board.size) {
        if (state.isDecided(cell) || state.domains[cell] == 0) continue
        var keep = state.domains[cell]
        for (mask in state.options(cell)) {
            if (state.closesLoop(cell, mask, groups)) keep = keep and (1 shl mask).inv()
        }
        if (keep != state.domains[cell]) return CircuitDeduction(CircuitTechnique.NO_LOOP, cell, keep)
    }
    return null
}

private fun CircuitSolverState.sealsOffGroup(cell: Int): Boolean {
    val seen = BooleanArray(board.size)
    val queue = ArrayDeque(listOf(cell))
    seen[cell] = true
    var count = 0
    var hasExit = false
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        count++
        for (direction in CIRCUIT_DIRECTIONS) {
            val other = board.neighbor(current, direction)
            if (other < 0 || seen[other]) continue
            if (linkForced(current, direction)) {
                seen[other] = true
                queue.add(other)
            } else if (linkPossible(current, direction)) {
                hasExit = true
            }
        }
    }
    return !hasExit && count < board.size
}

fun findCircuitDeadEnd(state: CircuitSolverState): CircuitDeduction? {
    for (cell in 0 until state.board.size) {
        if (state.isDecided(cell) || state.domains[cell] == 0) continue
        val original = state.domains[cell]
        var keep = original
        for (mask in state.options(cell)) {
            state.domains[cell] = 1 shl mask
            if (state.sealsOffGroup(cell)) keep = keep and (1 shl mask).inv()
        }
        state.domains[cell] = original
        if (keep != original) return CircuitDeduction(CircuitTechnique.DEAD_END, cell, keep)
    }
    return null
}

fun hasCircuitContradiction(state: CircuitSolverState): Boolean {
    if (state.hasEmptyDomain()) return true
    val board = state.board
    for (cell in 0 until board.size) {
        for (direction in CIRCUIT_DIRECTIONS) {
            if (!state.mustHave(cell, direction)) continue
            val other = board.neighbor(cell, direction)
            if (other < 0 || !state.canHave(other, oppositeCircuitDirection(direction))) return true
        }
    }
    val (groups, looped) = state.forcedComponents()
    if (looped) return true
    val hasExit = BooleanArray(board.size)
    for (cell in 0 until board.size) {
        val root = groups.find(cell)
        if (hasExit[root]) continue
        for (direction in CIRCUIT_DIRECTIONS) {
            val other = board.neighbor(cell, direction)
            if (other < 0 || groups.find(other) == root) continue
            if (state.linkPossible(cell, direction)) {
                hasExit[root] = true
                break
            }
        }
    }
    for (cell in 0 until board.size) {
        val root = groups.find(cell)
        if (root == cell && groups.sizes[root] < board.size && !hasExit[root]) return true
    }
    return false
}

private fun propagationRound(state: CircuitSolverState): Boolean {
    var changed = false
    for (cell in 0 until state.board.size) {
        if (state.domains[cell] == 0) return true
        changed = state.restrict(cell, state.edgeMatchKeep(cell)) or changed
    }
    if (changed) return true
    val next = findCircuitNoLoop(state) ?: findCircuitDeadEnd(state) ?: return false
    state.apply(next)
    return true
}

private fun contradictionDepth(state: CircuitSolverState, maxDepth: Int): Int? {
    var depth = 1
    while (true) {
        if (hasCircuitContradiction(state)) return depth
        if (depth >= maxDepth || !propagationRound(state)) return null
        depth++
    }
}

private fun singleStepChainLength(state: CircuitSolverState, cell: Int, mask: Int): Int {
    val hypothesis = state.copy()
    hypothesis.fix(cell, mask)
    var steps = 0
    while (!hasCircuitContradiction(hypothesis)) {
        val next = findCircuitEdgeMatch(hypothesis) ?: findCircuitNoLoop(hypothesis) ?: findCircuitDeadEnd(hypothesis)
            ?: return steps + CIRCUIT_LONG_WHAT_IF_CHAIN
        hypothesis.apply(next)
        steps++
    }
    return steps
}

private fun CircuitSolverState.singleDeductionKeeps(): List<Pair<Int, Int>> {
    val keeps = mutableListOf<Pair<Int, Int>>()
    val (groups, _) = forcedComponents()
    for (cell in 0 until board.size) {
        val original = domains[cell]
        if (original == 0) continue
        val edge = edgeMatchKeep(cell)
        if (edge != original) keeps += cell to edge
        if (isDecided(cell)) continue
        var loopKeep = original
        var deadKeep = original
        for (mask in options(cell)) {
            if (closesLoop(cell, mask, groups)) loopKeep = loopKeep and (1 shl mask).inv()
            domains[cell] = 1 shl mask
            if (sealsOffGroup(cell)) deadKeep = deadKeep and (1 shl mask).inv()
            domains[cell] = original
        }
        if (loopKeep != original) keeps += cell to loopKeep
        if (deadKeep != original) keeps += cell to deadKeep
    }
    return keeps
}

private fun refutationSteps(state: CircuitSolverState, cell: Int, mask: Int): Int {
    val hypothesis = state.copy()
    hypothesis.fix(cell, mask)
    if (hasCircuitContradiction(hypothesis)) return 0
    val oneStep = hypothesis.singleDeductionKeeps().any { (target, keep) ->
        val next = hypothesis.copy()
        next.restrict(target, keep)
        hasCircuitContradiction(next)
    }
    if (oneStep) return 1
    return maxOf(CIRCUIT_LONG_WHAT_IF_CHAIN, singleStepChainLength(state, cell, mask))
}

fun circuitWhatIfChainLength(state: CircuitSolverState, deduction: CircuitDeduction): Int {
    val removed = state.options(deduction.cell).filter { deduction.keep and (1 shl it) == 0 }
    return removed.minOfOrNull { refutationSteps(state, deduction.cell, it) } ?: 0
}

const val CIRCUIT_MAX_CHAIN_DEPTH = 10

fun findCircuitContradiction(state: CircuitSolverState, maxDepth: Int = CIRCUIT_MAX_CHAIN_DEPTH): CircuitDeduction? {
    var best: CircuitDeduction? = null
    for (cell in 0 until state.board.size) {
        if (state.isDecided(cell)) continue
        for (mask in state.options(cell)) {
            val limit = best?.chainDepth?.minus(1) ?: maxDepth
            if (limit < 1) return best
            val hypothesis = state.copy()
            hypothesis.fix(cell, mask)
            val depth = contradictionDepth(hypothesis, limit) ?: continue
            best = CircuitDeduction(CircuitTechnique.CONTRADICTION, cell, state.domains[cell] and (1 shl mask).inv(), depth)
        }
    }
    return best
}
