package com.quietgrid.engine.links

private const val LINKS_COLORING_MAX_NODES = 20_000

fun linksPairNeighbors(size: Int, solution: List<List<Int>>): List<Set<Int>> {
    val owner = IntArray(size * size) { -1 }
    solution.forEachIndexed { color, path -> path.forEach { owner[it] = color } }
    val neighbors = List(solution.size) { mutableSetOf<Int>() }
    for (cell in owner.indices) {
        val color = owner[cell]
        if (color < 0) continue
        for (next in linksNeighbors(cell, size)) {
            val other = owner[next]
            if (other >= 0 && other != color) neighbors[color] += other
        }
    }
    return neighbors
}

fun assignLinksPairColors(
    size: Int,
    solution: List<List<Int>>,
    paletteSize: Int,
    confusablePairs: Set<Pair<Int, Int>>,
): List<Int> {
    val pairCount = solution.size
    val neighbors = linksPairNeighbors(size, solution)
    val distinct = pairCount <= paletteSize
    val confusable = confusablePairs.flatMap { (a, b) -> listOf(a to b, b to a) }.toSet()
    val colors = IntArray(pairCount) { -1 }
    val used = BooleanArray(paletteSize)
    val order = (0 until pairCount).sortedByDescending { neighbors[it].size }
    var nodes = 0

    fun fits(pair: Int, color: Int): Boolean {
        if (distinct && used[color]) return false
        return neighbors[pair].none { other ->
            val otherColor = colors[other]
            otherColor >= 0 && (otherColor == color || (color to otherColor) in confusable)
        }
    }

    fun search(index: Int): Boolean {
        if (index == order.size) return true
        if (++nodes > LINKS_COLORING_MAX_NODES) return false
        val pair = order[index]
        for (offset in 0 until paletteSize) {
            val color = (pair + offset) % paletteSize
            if (!fits(pair, color)) continue
            colors[pair] = color
            if (distinct) used[color] = true
            if (search(index + 1)) return true
            colors[pair] = -1
            if (distinct) used[color] = false
        }
        return false
    }

    return if (search(0)) colors.toList() else List(pairCount) { it % paletteSize }
}
