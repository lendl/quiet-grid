package com.quietgrid.engine.flowfree

fun countFlowFreeSolutions(
    size: Int,
    pairCount: Int,
    starts: List<Pair<Int, Int>>,
    targets: List<Pair<Int, Int>>,
    cap: Int = 2,
    maxNodes: Int = 2000000,
): Int {
    val grid = Array(size) { IntArray(size) { -1 } }
    for (color in 0 until pairCount) {
        grid[starts[color].first][starts[color].second] = color
        grid[targets[color].first][targets[color].second] = color
    }
    val head = Array(pairCount) { starts[it] }
    var found = 0
    val nodeCount = intArrayOf(0)

    fun neighbors(row: Int, col: Int) =
        FLOWFREE_ORTHOGONAL_DELTAS.map { (dr, dc) -> (row + dr) to (col + dc) }.filter { (r, c) -> r in 0 until size && c in 0 until size }

    fun search(colorIndex: Int) {
        if (found >= cap) return
        nodeCount[0]++
        if (nodeCount[0] > maxNodes) return
        if (colorIndex == pairCount) {
            if (grid.all { row -> row.none { it == -1 } }) found++
            return
        }
        val target = targets[colorIndex]
        val current = head[colorIndex]
        if (current == target) {
            search(colorIndex + 1)
            return
        }
        val (row, col) = current
        for ((r, c) in neighbors(row, col)) {
            if (found >= cap) return
            val isTarget = (r to c) == target
            if (!isTarget && grid[r][c] != -1) continue
            val previous = grid[r][c]
            grid[r][c] = colorIndex
            head[colorIndex] = r to c
            search(colorIndex)
            grid[r][c] = previous
            head[colorIndex] = current
        }
    }

    search(0)
    return found
}
