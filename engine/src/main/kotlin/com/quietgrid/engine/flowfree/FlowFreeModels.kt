package com.quietgrid.engine.flowfree

import kotlinx.serialization.Serializable

@Serializable
data class FlowFreePuzzleEntry(
    val id: String,
    val size: Int,
    val difficulty: String,
    val pairCount: Int,
    val endpoints: List<List<Int>>,
    val paths: List<List<Int>>,
)

internal val FLOWFREE_ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun flowFreeEncodeCell(row: Int, col: Int, size: Int): Int = row * size + col

fun flowFreeDecodeCell(index: Int, size: Int): Pair<Int, Int> = (index / size) to (index % size)

fun isValidFlowFreePaths(size: Int, pairCount: Int, paths: List<List<Int>>): Boolean {
    if (paths.size != pairCount) return false
    if (paths.any { it.size < 2 }) return false

    val seenCells = HashSet<Int>()
    for (path in paths) {
        if (path.any { it !in 0 until size * size }) return false
        if (path.toSet().size != path.size) return false
        for (i in 0 until path.size - 1) {
            val (r1, c1) = flowFreeDecodeCell(path[i], size)
            val (r2, c2) = flowFreeDecodeCell(path[i + 1], size)
            if (kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) != 1) return false
        }
        for (cell in path) {
            if (!seenCells.add(cell)) return false
        }
    }
    return seenCells.size == size * size
}

fun endpointsOf(size: Int, pairCount: Int, paths: List<List<Int>>): List<List<Int>> {
    val grid = MutableList(size) { MutableList(size) { -1 } }
    for (color in 0 until pairCount) {
        val path = paths[color]
        val (startRow, startCol) = flowFreeDecodeCell(path.first(), size)
        val (endRow, endCol) = flowFreeDecodeCell(path.last(), size)
        grid[startRow][startCol] = color
        grid[endRow][endCol] = color
    }
    return grid.map { it.toList() }
}
