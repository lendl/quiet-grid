package com.quietgrid.engine.flowfree

import kotlin.random.Random

fun generateHamiltonianPath(
    size: Int,
    random: Random = Random.Default,
    maxAttempts: Int = 200,
    stepBudgetPerAttempt: Int = 20000,
): List<Pair<Int, Int>>? {
    repeat(maxAttempts) {
        val start = random.nextInt(size) to random.nextInt(size)
        val path = mutableListOf(start)
        val visited = Array(size) { BooleanArray(size) }
        visited[start.first][start.second] = true
        val steps = intArrayOf(0)
        if (extendHamiltonianPath(size, path, visited, random, steps, stepBudgetPerAttempt)) {
            return path.toList()
        }
    }
    return null
}

private fun extendHamiltonianPath(
    size: Int,
    path: MutableList<Pair<Int, Int>>,
    visited: Array<BooleanArray>,
    random: Random,
    steps: IntArray,
    stepBudget: Int,
): Boolean {
    if (path.size == size * size) return true
    steps[0]++
    if (steps[0] > stepBudget) return false

    val (row, col) = path.last()
    val candidates = FLOWFREE_ORTHOGONAL_DELTAS
        .map { (dr, dc) -> (row + dr) to (col + dc) }
        .filter { (r, c) -> r in 0 until size && c in 0 until size && !visited[r][c] }
        .shuffled(random)
        .sortedBy { (r, c) -> onwardOptionCount(size, r, c, visited) }

    for ((r, c) in candidates) {
        visited[r][c] = true
        path.add(r to c)
        if (extendHamiltonianPath(size, path, visited, random, steps, stepBudget)) return true
        path.removeAt(path.size - 1)
        visited[r][c] = false
    }
    return false
}

private fun onwardOptionCount(size: Int, row: Int, col: Int, visited: Array<BooleanArray>): Int =
    FLOWFREE_ORTHOGONAL_DELTAS.count { (dr, dc) ->
        val r = row + dr
        val c = col + dc
        r in 0 until size && c in 0 until size && !visited[r][c]
    }

fun partitionIntoPaths(
    path: List<Pair<Int, Int>>,
    pairCount: Int,
    random: Random = Random.Default,
    maxAttempts: Int = 500,
): List<List<Pair<Int, Int>>>? {
    val total = path.size
    require(pairCount in 1..total / 2) { "pairCount must allow every segment at least 2 cells" }
    repeat(maxAttempts) {
        val cuts = (1 until total).shuffled(random).take(pairCount - 1).sorted()
        val boundaries = listOf(0) + cuts + listOf(total)
        val segments = (0 until boundaries.size - 1).map { i -> path.subList(boundaries[i], boundaries[i + 1]).toList() }
        if (segments.all { it.size >= 2 }) return segments
    }
    return null
}

fun generateFlowFreeSolution(size: Int, pairCount: Int, random: Random = Random.Default): List<List<Int>>? {
    val hamiltonianPath = generateHamiltonianPath(size, random) ?: return null
    val segments = partitionIntoPaths(hamiltonianPath, pairCount, random) ?: return null
    return segments.map { segment -> segment.map { (r, c) -> flowFreeEncodeCell(r, c, size) } }
}
