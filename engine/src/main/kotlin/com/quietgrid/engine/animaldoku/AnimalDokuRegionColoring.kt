package com.quietgrid.engine.animaldoku

fun assignRegionColors(
    regions: List<List<Int>>,
    paletteSize: Int,
    forbiddenPairs: Set<Pair<Int, Int>>,
): List<Int> {
    val regionCount = (regions.flatten().maxOrNull() ?: -1) + 1
    val indexColors = List(regionCount) { it % paletteSize }
    if (regionCount == 0 || regionCount > paletteSize) return indexColors

    val neighbors = regionNeighbors(regions, regionCount)
    val forbidden = forbiddenPairs.map { (a, b) -> minOf(a, b) to maxOf(a, b) }.toSet()
    fun clashes(colorA: Int, colorB: Int): Boolean = (minOf(colorA, colorB) to maxOf(colorA, colorB)) in forbidden
    fun candidateOrder(region: Int): List<Int> = listOf(region % paletteSize) + (0 until paletteSize).filter { it != region % paletteSize }

    val colors = IntArray(regionCount) { -1 }
    val used = BooleanArray(paletteSize)

    fun addedClashes(region: Int, color: Int): Int =
        neighbors[region].count { neighbor -> colors[neighbor] >= 0 && clashes(color, colors[neighbor]) }

    fun assignWithoutClashes(region: Int): Boolean {
        if (region == regionCount) return true
        for (color in candidateOrder(region)) {
            if (used[color] || addedClashes(region, color) > 0) continue
            colors[region] = color
            used[color] = true
            if (assignWithoutClashes(region + 1)) return true
            colors[region] = -1
            used[color] = false
        }
        return false
    }

    if (assignWithoutClashes(0)) return colors.toList()

    var best = indexColors
    var bestClashes = Int.MAX_VALUE

    fun searchFewestClashes(region: Int, clashesSoFar: Int) {
        if (clashesSoFar >= bestClashes) return
        if (region == regionCount) {
            best = colors.toList()
            bestClashes = clashesSoFar
            return
        }
        for (color in candidateOrder(region)) {
            if (used[color]) continue
            val added = addedClashes(region, color)
            colors[region] = color
            used[color] = true
            searchFewestClashes(region + 1, clashesSoFar + added)
            colors[region] = -1
            used[color] = false
        }
    }

    searchFewestClashes(0, 0)
    return best
}

private fun regionNeighbors(regions: List<List<Int>>, regionCount: Int): List<Set<Int>> {
    val neighbors = List(regionCount) { mutableSetOf<Int>() }
    for (row in regions.indices) {
        for (col in regions[row].indices) {
            val region = regions[row][col]
            val below = regions.getOrNull(row + 1)?.getOrNull(col)
            val right = regions[row].getOrNull(col + 1)
            for (other in listOfNotNull(below, right)) {
                if (other != region) {
                    neighbors[region] += other
                    neighbors[other] += region
                }
            }
        }
    }
    return neighbors
}
