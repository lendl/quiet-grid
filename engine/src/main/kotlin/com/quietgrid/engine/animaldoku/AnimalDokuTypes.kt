package com.quietgrid.engine.animaldoku

import kotlinx.serialization.Serializable

@Serializable
data class AnimalDokuPuzzleEntry(
    val id: String,
    val size: Int,
    val difficulty: String,
    val regions: List<List<Int>>,
    val solution: List<Int>,
)

private val ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun isValidAnimalDokuRegionGrid(size: Int, regions: List<List<Int>>): Boolean {
    if (regions.size != size || regions.any { it.size != size }) return false

    val cellsByRegion = HashMap<Int, MutableList<Pair<Int, Int>>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            val region = regions[row][col]
            if (region !in 0 until size) return false
            cellsByRegion.getOrPut(region) { mutableListOf() }.add(row to col)
        }
    }
    if (cellsByRegion.keys != (0 until size).toSet()) return false

    return cellsByRegion.values.all { cells -> isConnected(cells) }
}

private fun isConnected(cells: List<Pair<Int, Int>>): Boolean {
    if (cells.isEmpty()) return false
    val cellSet = cells.toHashSet()
    val visited = hashSetOf(cells.first())
    val queue = ArrayDeque(listOf(cells.first()))
    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeFirst()
        for ((dr, dc) in ORTHOGONAL_DELTAS) {
            val neighbor = (row + dr) to (col + dc)
            if (neighbor in cellSet && neighbor !in visited) {
                visited.add(neighbor)
                queue.add(neighbor)
            }
        }
    }
    return visited.size == cells.size
}
