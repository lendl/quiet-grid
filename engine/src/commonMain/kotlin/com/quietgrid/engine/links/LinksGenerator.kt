package com.quietgrid.engine.links

import kotlin.random.Random

fun generateLinksSolution(size: Int, pairCount: Int, random: Random = Random.Default): List<List<Int>>? {
    val cellCount = size * size
    require(pairCount in 1..cellCount / LINKS_MIN_PATH_LENGTH)
    val paths = HashMap<Int, ArrayDeque<Int>>()
    val pathOf = IntArray(cellCount) { it }
    for (cell in 0 until cellCount) paths[cell] = ArrayDeque(listOf(cell))
    var nextId = cellCount

    while (paths.size > pairCount) {
        val order = paths.keys.shuffled(random).sortedBy { paths.getValue(it).size }
        var merged = false
        for (id in order) {
            val path = paths.getValue(id)
            val options = mutableListOf<Triple<Int, Int, Int>>()
            for (end in pathEnds(path)) {
                for (next in linksNeighbors(end, size)) {
                    val other = pathOf[next]
                    if (other == id) continue
                    val otherPath = paths.getValue(other)
                    if (next !in pathEnds(otherPath)) continue
                    if (touchesElsewhere(otherPath, end, next, pathOf, id, size)) continue
                    options += Triple(end, next, other)
                }
            }
            if (options.isEmpty()) continue
            val shortest = options.minOf { paths.getValue(it.third).size }
            val (end, next, other) = options.filter { paths.getValue(it.third).size == shortest }.random(random)
            val joined = ArrayDeque<Int>()
            joined.addAll(if (path.last() == end) path else path.reversed())
            val otherPath = paths.getValue(other)
            joined.addAll(if (otherPath.first() == next) otherPath else otherPath.reversed())
            paths.remove(id)
            paths.remove(other)
            val newId = nextId++
            paths[newId] = joined
            joined.forEach { pathOf[it] = newId }
            merged = true
            break
        }
        if (!merged) return null
    }
    val result = paths.values.map { it.toList() }
    if (result.any { it.size < LINKS_MIN_PATH_LENGTH }) return null
    return result.sortedBy { it.first() }
}

private fun pathEnds(path: ArrayDeque<Int>): List<Int> =
    if (path.size == 1) listOf(path.first()) else listOf(path.first(), path.last())

private fun touchesElsewhere(
    otherPath: ArrayDeque<Int>,
    end: Int,
    next: Int,
    pathOf: IntArray,
    id: Int,
    size: Int,
): Boolean {
    for (cell in otherPath) {
        for (neighbor in linksNeighbors(cell, size)) {
            if (pathOf[neighbor] == id && !(cell == next && neighbor == end)) return true
        }
    }
    return false
}

fun linksEndpointsOf(solution: List<List<Int>>): List<List<Int>> = solution.map { listOf(it.first(), it.last()) }

fun isValidLinksSolution(size: Int, endpoints: List<List<Int>>, solution: List<List<Int>>): Boolean {
    if (solution.size != endpoints.size) return false
    val seen = BooleanArray(size * size)
    solution.forEachIndexed { color, path ->
        if (path.size < 2) return false
        val ends = endpoints[color]
        if (path.first() != ends[0] || path.last() != ends[1]) return false
        path.forEachIndexed { index, cell ->
            if (cell !in seen.indices || seen[cell]) return false
            seen[cell] = true
            if (index > 0 && !linksAreAdjacent(path[index - 1], cell, size)) return false
        }
    }
    return seen.all { it }
}
