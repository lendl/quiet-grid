package com.quietgrid.engine.flowfree

enum class FlowFreeTechnique { FORCED_CONTINUATION, REGION_ISOLATION, BRANCH_SEARCH }

data class FlowFreeSolveStep(val technique: FlowFreeTechnique, val color: Int, val branchWidth: Int)

data class FlowFreeSolveResult(val solved: Boolean, val steps: List<FlowFreeSolveStep>, val solutionPaths: List<List<Int>>?)

private class FlowFreeSolverState(val size: Int, val pairCount: Int, val starts: List<Pair<Int, Int>>, val targets: List<Pair<Int, Int>>) {
    val grid: Array<IntArray> = Array(size) { IntArray(size) { -1 } }
    val head: Array<Pair<Int, Int>> = Array(pairCount) { starts[it] }
    val finished: BooleanArray = BooleanArray(pairCount)
    val pathCells: Array<MutableList<Pair<Int, Int>>> = Array(pairCount) { mutableListOf(starts[it]) }

    init {
        for (color in 0 until pairCount) {
            grid[starts[color].first][starts[color].second] = color
            grid[targets[color].first][targets[color].second] = color
        }
    }

    fun isEmpty(row: Int, col: Int) = grid[row][col] == -1

    fun neighbors(row: Int, col: Int): List<Pair<Int, Int>> =
        FLOWFREE_ORTHOGONAL_DELTAS.map { (dr, dc) -> (row + dr) to (col + dc) }.filter { (r, c) -> r in 0 until size && c in 0 until size }
}

private fun targetReachable(state: FlowFreeSolverState, color: Int): Boolean {
    val target = state.targets[color]
    val start = state.head[color]
    if (start == target) return true
    val visited = hashSetOf(start)
    val queue = ArrayDeque(listOf(start))
    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeFirst()
        for ((r, c) in state.neighbors(row, col)) {
            val key = r to c
            if (key in visited) continue
            if (key == target) return true
            if (!state.isEmpty(r, c)) continue
            visited.add(key)
            queue.add(key)
        }
    }
    return false
}

private fun allEmptyCellsReachableFromSomeHead(state: FlowFreeSolverState): Boolean {
    val activeHeads = (0 until state.pairCount).filter { !state.finished[it] }.map { state.head[it] }
    val visited = HashSet<Pair<Int, Int>>()
    val queue = ArrayDeque<Pair<Int, Int>>()
    activeHeads.forEach { if (visited.add(it)) queue.add(it) }
    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeFirst()
        for ((r, c) in state.neighbors(row, col)) {
            val key = r to c
            if (key in visited || !state.isEmpty(r, c)) continue
            visited.add(key)
            queue.add(key)
        }
    }
    for (row in 0 until state.size) for (col in 0 until state.size) {
        if (state.isEmpty(row, col) && (row to col) !in visited) return false
    }
    return true
}

fun solveFlowFree(size: Int, pairCount: Int, starts: List<Pair<Int, Int>>, targets: List<Pair<Int, Int>>, maxNodes: Int = 200000): FlowFreeSolveResult {
    val state = FlowFreeSolverState(size, pairCount, starts, targets)
    val order = (0 until pairCount).sortedBy { manhattan(starts[it], targets[it]) }
    val steps = mutableListOf<FlowFreeSolveStep>()
    val nodeCount = intArrayOf(0)
    val solved = solveColor(state, order, 0, steps, nodeCount, maxNodes)
    val solutionPaths = if (solved) state.pathCells.map { cells -> cells.map { (r, c) -> flowFreeEncodeCell(r, c, size) } } else null
    return FlowFreeSolveResult(solved, steps, solutionPaths)
}

private fun manhattan(a: Pair<Int, Int>, b: Pair<Int, Int>) = kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)

private fun solveColor(state: FlowFreeSolverState, order: List<Int>, orderIndex: Int, steps: MutableList<FlowFreeSolveStep>, nodeCount: IntArray, maxNodes: Int): Boolean {
    if (orderIndex == order.size) return state.grid.all { row -> row.none { it == -1 } }
    return extendPath(state, order[orderIndex], order, orderIndex, steps, nodeCount, maxNodes)
}

private fun extendPath(state: FlowFreeSolverState, color: Int, order: List<Int>, orderIndex: Int, steps: MutableList<FlowFreeSolveStep>, nodeCount: IntArray, maxNodes: Int): Boolean {
    nodeCount[0]++
    if (nodeCount[0] > maxNodes) return false

    val current = state.head[color]
    val target = state.targets[color]
    if (current == target) {
        state.finished[color] = true
        val advanced = solveColor(state, order, orderIndex + 1, steps, nodeCount, maxNodes)
        if (!advanced) state.finished[color] = false
        return advanced
    }

    val (row, col) = current
    val rawCandidates = state.neighbors(row, col).filter { (r, c) -> (r to c) == target || state.isEmpty(r, c) }
    val candidates = rawCandidates.filter { (r, c) ->
        state.grid[r][c] = color
        state.head[color] = r to c
        val ok = targetReachable(state, color) && allEmptyCellsReachableFromSomeHead(state)
        state.grid[r][c] = if ((r to c) == target) color else -1
        state.head[color] = current
        ok
    }

    if (candidates.isEmpty()) return false

    for ((index, cell) in candidates.withIndex()) {
        val (r, c) = cell
        val requiredBacktrack = index > 0
        val technique = when {
            requiredBacktrack -> FlowFreeTechnique.BRANCH_SEARCH
            rawCandidates.size > candidates.size -> FlowFreeTechnique.REGION_ISOLATION
            else -> FlowFreeTechnique.FORCED_CONTINUATION
        }
        state.grid[r][c] = color
        state.head[color] = r to c
        state.pathCells[color].add(r to c)
        steps.add(FlowFreeSolveStep(technique, color, index + 1))
        if (extendPath(state, color, order, orderIndex, steps, nodeCount, maxNodes)) return true
        steps.removeAt(steps.size - 1)
        state.pathCells[color].removeAt(state.pathCells[color].size - 1)
        state.grid[r][c] = if ((r to c) == target) color else -1
        state.head[color] = current
    }
    return false
}
