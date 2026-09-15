package com.quietgrid.cli.starbattle

import com.quietgrid.engine.starbattle.StarBattleSolveResult
import com.quietgrid.engine.starbattle.solveStarBattle

private val ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun growStarBattleRegions(size: Int, solution: List<List<Int>>): List<List<Int>>? {
    val regionOf = Array(size) { IntArray(size) { -1 } }
    val claimed = List(size) { mutableListOf<Pair<Int, Int>>() }
    for (row in 0 until size) {
        val cols = solution[row]
        for (col in cols.min()..cols.max()) {
            if (regionOf[row][col] == -1) {
                regionOf[row][col] = row
                claimed[row].add(row to col)
            }
        }
    }
    var unclaimed = size * size - claimed.sumOf { it.size }

    while (unclaimed > 0) {
        val order = (0 until size).shuffled()
        var progressed = false
        for (regionId in order) {
            if (unclaimed == 0) break
            val candidates = mutableSetOf<Pair<Int, Int>>()
            for ((r, c) in claimed[regionId]) {
                for ((dr, dc) in ORTHOGONAL_DELTAS) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until size && nc in 0 until size && regionOf[nr][nc] == -1) candidates.add(nr to nc)
                }
            }
            if (candidates.isEmpty()) continue
            val (cr, cc) = candidates.random()
            regionOf[cr][cc] = regionId
            claimed[regionId].add(cr to cc)
            unclaimed--
            progressed = true
        }
        if (!progressed) return null
    }
    return regionOf.map { it.toList() }
}

data class StarBattleRepairedPuzzle(val regions: List<List<Int>>, val solveResult: StarBattleSolveResult)

fun repairStarBattleRegionsTowardUniqueSolution(
    size: Int,
    k: Int,
    solution: List<List<Int>>,
    initialRegions: List<List<Int>>,
    maxRepairAttempts: Int = 50,
): StarBattleRepairedPuzzle? {
    var regions = initialRegions
    repeat(maxRepairAttempts) {
        val result = solveStarBattle(size, k, regions)
        if (result.solved) return StarBattleRepairedPuzzle(regions, result)
        regions = mutateOneStarBattleBoundaryCell(size, solution, regions) ?: return@repeat
    }
    val finalResult = solveStarBattle(size, k, regions)
    return if (finalResult.solved) StarBattleRepairedPuzzle(regions, finalResult) else null
}

internal fun isConnectedWithoutStarBattleCell(size: Int, regionOf: List<List<Int>>, region: Int, excluded: Pair<Int, Int>): Boolean {
    val cells = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until size) for (c in 0 until size) {
        if (regionOf[r][c] == region && (r to c) != excluded) cells.add(r to c)
    }
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

internal const val MIN_DONOR_REGION_SIZE_TO_PREFER_SB = 3

internal fun mutateOneStarBattleBoundaryCell(
    size: Int,
    solution: List<List<Int>>,
    regionOf: List<List<Int>>,
    minDonorRegionSizeToPrefer: Int = MIN_DONOR_REGION_SIZE_TO_PREFER_SB,
): List<List<Int>>? {
    val regionSizes = IntArray(size)
    for (row in 0 until size) for (col in 0 until size) regionSizes[regionOf[row][col]]++

    val balancedCandidates = mutableListOf<Triple<Int, Int, Int>>()
    val allCandidates = mutableListOf<Triple<Int, Int, Int>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (col in solution[row]) continue
            val currentRegion = regionOf[row][col]
            val neighborRegions = mutableListOf<Int>()
            for ((dr, dc) in ORTHOGONAL_DELTAS) {
                val nr = row + dr
                val nc = col + dc
                if (nr !in 0 until size || nc !in 0 until size) continue
                val neighborRegion = regionOf[nr][nc]
                if (neighborRegion != currentRegion) neighborRegions.add(neighborRegion)
            }
            if (neighborRegions.isEmpty()) continue
            if (!isConnectedWithoutStarBattleCell(size, regionOf, currentRegion, row to col)) continue
            val donorIsBigEnough = regionSizes[currentRegion] >= minDonorRegionSizeToPrefer
            for (neighborRegion in neighborRegions) {
                val candidate = Triple(row, col, neighborRegion)
                allCandidates.add(candidate)
                if (donorIsBigEnough) balancedCandidates.add(candidate)
            }
        }
    }
    val pool = balancedCandidates.ifEmpty { allCandidates }
    if (pool.isEmpty()) return null
    val (row, col, newRegion) = pool.random()
    return regionOf.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c != col) v else newRegion } }
}
