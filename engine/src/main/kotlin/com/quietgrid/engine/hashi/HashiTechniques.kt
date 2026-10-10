package com.quietgrid.engine.hashi

enum class HashiTechnique { CROSSING, CAPACITY, ISOLATION, CONTRADICTION }

data class HashiDeduction(
    val technique: HashiTechnique,
    val restrictions: Map<Int, Int>,
    val chainDepth: Int = 0,
)

fun HashiSolverState.apply(deduction: HashiDeduction) {
    deduction.restrictions.forEach { (edge, mask) -> restrict(edge, mask) }
}

fun findHashiCrossing(state: HashiSolverState): HashiDeduction? {
    val board = state.board
    for (edge in board.edges.indices) {
        if (state.domains[edge] == 0 || state.lo(edge) == 0) continue
        val blocked = board.crossings[edge].filter { state.hi(it) > 0 }
        if (blocked.isNotEmpty()) return HashiDeduction(HashiTechnique.CROSSING, blocked.associateWith { 1 })
    }
    return null
}

private fun capacityRestrictions(state: HashiSolverState, island: Int, loTotals: IntArray, hiTotals: IntArray): Map<Int, Int> {
    val board = state.board
    val need = board.islands[island].count
    val changes = HashMap<Int, Int>()
    for (edge in board.edgesOfIsland[island]) {
        val othersLo = loTotals[island] - state.lo(edge)
        val othersHi = hiTotals[island] - state.hi(edge)
        var mask = 0
        for (value in 0..HASHI_MAX_BRIDGES_PER_EDGE) {
            if (state.allows(edge, value) && need - value in othersLo..othersHi) mask = mask or (1 shl value)
        }
        if (mask != state.domains[edge]) changes[edge] = mask
    }
    return changes
}

fun findHashiCapacity(state: HashiSolverState): HashiDeduction? {
    val loTotals = state.loTotals()
    val hiTotals = state.hiTotals()
    for (island in state.board.islands.indices) {
        val changes = capacityRestrictions(state, island, loTotals, hiTotals)
        if (changes.isNotEmpty()) return HashiDeduction(HashiTechnique.CAPACITY, changes)
    }
    return null
}

private fun componentOf(state: HashiSolverState, start: Int, extraEdge: Int = -1): IntArray {
    val board = state.board
    val visited = BooleanArray(board.islands.size)
    val members = ArrayList<Int>()
    val queue = ArrayDeque(listOf(start))
    visited[start] = true
    while (queue.isNotEmpty()) {
        val island = queue.removeFirst()
        members += island
        for (edge in board.edgesOfIsland[island]) {
            if (edge != extraEdge && state.lo(edge) == 0) continue
            val other = board.otherEnd(edge, island)
            if (!visited[other]) {
                visited[other] = true
                queue.add(other)
            }
        }
    }
    return members.toIntArray()
}

private fun closesIsolatedGroup(state: HashiSolverState, edge: Int, value: Int, loTotals: IntArray): Boolean {
    val board = state.board
    val ends = board.edges[edge]
    val delta = value - state.lo(edge)
    if (loTotals[ends.a] + delta != board.islands[ends.a].count) return false
    if (loTotals[ends.b] + delta != board.islands[ends.b].count) return false
    val group = componentOf(state, ends.a, extraEdge = edge)
    if (group.size == board.islands.size) return false
    return group.all { island ->
        val total = if (island == ends.a || island == ends.b) loTotals[island] + delta else loTotals[island]
        total == board.islands[island].count
    }
}

fun findHashiIsolation(state: HashiSolverState): HashiDeduction? {
    val board = state.board
    val loTotals = state.loTotals()
    for (edge in board.edges.indices) {
        if (state.isDecided(edge)) continue
        var mask = state.domains[edge]
        for (value in 1..HASHI_MAX_BRIDGES_PER_EDGE) {
            if (state.allows(edge, value) && closesIsolatedGroup(state, edge, value, loTotals)) mask = mask and (1 shl value).inv()
        }
        if (mask != state.domains[edge]) return HashiDeduction(HashiTechnique.ISOLATION, mapOf(edge to mask))
    }

    val seen = BooleanArray(board.islands.size)
    for (start in board.islands.indices) {
        if (seen[start]) continue
        val group = componentOf(state, start)
        group.forEach { seen[it] = true }
        if (group.size == board.islands.size) continue
        val inGroup = BooleanArray(board.islands.size).also { flags -> group.forEach { flags[it] = true } }
        val exits = group.flatMap { island -> board.edgesOfIsland[island].filter { !inGroup[board.otherEnd(it, island)] && state.hi(it) > 0 } }.distinct()
        if (exits.size == 1 && state.allows(exits.single(), 0)) {
            return HashiDeduction(HashiTechnique.ISOLATION, mapOf(exits.single() to (state.domains[exits.single()] and 1.inv())))
        }
    }
    return null
}

fun hasHashiContradiction(state: HashiSolverState): Boolean {
    if (state.hasEmptyDomain()) return true
    val board = state.board
    val loTotals = state.loTotals()
    val hiTotals = state.hiTotals()
    for (island in board.islands.indices) {
        if (board.islands[island].count !in loTotals[island]..hiTotals[island]) return true
    }
    for (edge in board.edges.indices) {
        if (state.lo(edge) > 0 && board.crossings[edge].any { state.lo(it) > 0 }) return true
    }
    val seen = BooleanArray(board.islands.size)
    for (start in board.islands.indices) {
        if (seen[start]) continue
        val group = componentOf(state, start)
        group.forEach { seen[it] = true }
        if (group.size == board.islands.size) continue
        val inGroup = BooleanArray(board.islands.size).also { flags -> group.forEach { flags[it] = true } }
        val hasExit = group.any { island -> board.edgesOfIsland[island].any { !inGroup[board.otherEnd(it, island)] && state.hi(it) > 0 } }
        if (!hasExit) return true
    }
    return false
}

private fun propagationRound(state: HashiSolverState): Boolean {
    var changed = false
    for (edge in state.board.edges.indices) {
        if (state.domains[edge] == 0 || state.lo(edge) == 0) continue
        for (other in state.board.crossings[edge]) changed = state.restrict(other, 1) or changed
    }
    val loTotals = state.loTotals()
    val hiTotals = state.hiTotals()
    for (island in state.board.islands.indices) {
        capacityRestrictions(state, island, loTotals, hiTotals).forEach { (edge, mask) -> changed = state.restrict(edge, mask) or changed }
    }
    if (changed) return true
    val isolation = findHashiIsolation(state) ?: return false
    state.apply(isolation)
    return true
}

private fun contradictionDepth(state: HashiSolverState, maxDepth: Int): Int? {
    var depth = 1
    while (true) {
        if (hasHashiContradiction(state)) return depth
        if (depth >= maxDepth || !propagationRound(state)) return null
        depth++
    }
}

const val HASHI_MAX_CHAIN_DEPTH = 12

fun findHashiContradiction(state: HashiSolverState, maxDepth: Int = HASHI_MAX_CHAIN_DEPTH): HashiDeduction? {
    var best: HashiDeduction? = null
    for (edge in state.board.edges.indices) {
        if (state.isDecided(edge)) continue
        for (value in 0..HASHI_MAX_BRIDGES_PER_EDGE) {
            if (!state.allows(edge, value)) continue
            val limit = best?.chainDepth?.minus(1) ?: maxDepth
            if (limit < 1) return best
            val hypothesis = state.copy()
            hypothesis.fix(edge, value)
            val depth = contradictionDepth(hypothesis, limit) ?: continue
            best = HashiDeduction(HashiTechnique.CONTRADICTION, mapOf(edge to (state.domains[edge] and (1 shl value).inv())), depth)
        }
    }
    return best
}
