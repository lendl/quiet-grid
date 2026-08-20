package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import com.quietgrid.engine.core.Difficulty
import kotlin.math.abs

fun generateSolutionPermutation(size: Int, maxAttempts: Int = 500): IntArray? {
    repeat(maxAttempts) {
        val perm = (0 until size).shuffled().toIntArray()
        var valid = true
        for (row in 0 until size - 1) {
            if (abs(perm[row] - perm[row + 1]) <= 1) {
                valid = false
                break
            }
        }
        if (valid) return perm
    }
    return null
}

private val ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun growRegions(size: Int, solution: IntArray): List<List<Int>>? {
    val regionOf = Array(size) { IntArray(size) { -1 } }
    val claimed = List(size) { mutableListOf<Pair<Int, Int>>() }
    for (row in 0 until size) {
        val col = solution[row]
        regionOf[row][col] = row
        claimed[row].add(row to col)
    }
    var unclaimed = size * size - size

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

data class AnimalDokuRepairedPuzzle(val regions: List<List<Int>>, val solveResult: AnimalDokuSolveResult)

fun repairRegionsTowardUniqueSolution(
    size: Int,
    solution: IntArray,
    initialRegions: List<List<Int>>,
    maxRepairAttempts: Int = 50,
): AnimalDokuRepairedPuzzle? {
    var regions = initialRegions
    repeat(maxRepairAttempts) {
        val result = solveAnimalDoku(size, regions)
        if (result.solved) return AnimalDokuRepairedPuzzle(regions, result)
        regions = mutateOneBoundaryCell(size, solution, regions) ?: return@repeat
    }
    val finalResult = solveAnimalDoku(size, regions)
    return if (finalResult.solved) AnimalDokuRepairedPuzzle(regions, finalResult) else null
}

internal fun isConnectedWithoutCell(size: Int, regionOf: List<List<Int>>, region: Int, excluded: Pair<Int, Int>): Boolean {
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

internal const val MIN_DONOR_REGION_SIZE_TO_PREFER = 3

internal fun boundaryMutationCandidates(
    size: Int,
    solution: IntArray,
    regionOf: List<List<Int>>,
    minDonorRegionSizeToPrefer: Int = MIN_DONOR_REGION_SIZE_TO_PREFER,
): List<Triple<Int, Int, Int>> {
    val regionSizes = IntArray(size)
    for (row in 0 until size) for (col in 0 until size) regionSizes[regionOf[row][col]]++

    val balancedCandidates = mutableListOf<Triple<Int, Int, Int>>()
    val allCandidates = mutableListOf<Triple<Int, Int, Int>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (col == solution[row]) continue
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
            if (!isConnectedWithoutCell(size, regionOf, currentRegion, row to col)) continue
            val donorIsBigEnough = regionSizes[currentRegion] >= minDonorRegionSizeToPrefer
            for (neighborRegion in neighborRegions) {
                val candidate = Triple(row, col, neighborRegion)
                allCandidates.add(candidate)
                if (donorIsBigEnough) balancedCandidates.add(candidate)
            }
        }
    }
    return balancedCandidates.ifEmpty { allCandidates }
}

internal fun mutateOneBoundaryCell(
    size: Int,
    solution: IntArray,
    regionOf: List<List<Int>>,
    minDonorRegionSizeToPrefer: Int = MIN_DONOR_REGION_SIZE_TO_PREFER,
): List<List<Int>>? {
    val pool = boundaryMutationCandidates(size, solution, regionOf, minDonorRegionSizeToPrefer)
    if (pool.isEmpty()) return null
    val (row, col, newRegion) = pool.random()
    return regionOf.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c != col) v else newRegion } }
}

internal fun donationCandidatesFrom(
    size: Int,
    solution: IntArray,
    regionOf: List<List<Int>>,
    donorRegion: Int,
): List<Triple<Int, Int, Int>> {
    val candidates = mutableListOf<Triple<Int, Int, Int>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (regionOf[row][col] != donorRegion) continue
            if (col == solution[row]) continue
            val neighborRegions = mutableListOf<Int>()
            for ((dr, dc) in ORTHOGONAL_DELTAS) {
                val nr = row + dr
                val nc = col + dc
                if (nr !in 0 until size || nc !in 0 until size) continue
                val neighborRegion = regionOf[nr][nc]
                if (neighborRegion != donorRegion) neighborRegions.add(neighborRegion)
            }
            if (neighborRegions.isEmpty()) continue
            if (!isConnectedWithoutCell(size, regionOf, donorRegion, row to col)) continue
            for (neighborRegion in neighborRegions) {
                candidates.add(Triple(row, col, neighborRegion))
            }
        }
    }
    return candidates
}

fun forceOneSingleCellRegion(size: Int, solution: IntArray, regions: List<List<Int>>): List<List<Int>>? {
    val initialSizes = IntArray(size)
    for (row in 0 until size) for (col in 0 until size) initialSizes[regions[row][col]]++
    val targetRegion = (0 until size).minByOrNull { initialSizes[it] } ?: return null

    var current = regions
    var remaining = initialSizes[targetRegion]
    while (remaining > 1) {
        val candidates = donationCandidatesFrom(size, solution, current, targetRegion)
        if (candidates.isEmpty()) return null
        val (row, col, newRegion) = candidates.random()
        current = current.mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, v -> if (c != col) v else newRegion } }
        remaining--
    }
    return current
}

private const val REPAIR_ATTEMPTS_FOR_GENERATION = 400

internal fun defaultMaxAttemptsFor(targetDifficulty: Difficulty): Int = when (targetDifficulty) {
    Difficulty.HARD -> 50
    Difficulty.EXPERT -> 20
    else -> 300
}

fun generateAnimalDokuPuzzleForSolution(
    size: Int,
    solution: IntArray,
    targetDifficulty: Difficulty,
    idPrefix: String,
    maxRegionAttempts: Int = defaultMaxAttemptsFor(targetDifficulty),
): AnimalDokuPuzzleEntry? {
    repeat(maxRegionAttempts) {
        val initialRegions = growRegions(size, solution) ?: return@repeat
        val repaired = repairRegionsTowardUniqueSolution(size, solution, initialRegions, REPAIR_ATTEMPTS_FOR_GENERATION) ?: return@repeat

        var finalRegions = repaired.regions
        var finalSolveResult = repaired.solveResult

        if (targetDifficulty == Difficulty.HARD || targetDifficulty == Difficulty.EXPERT) {
            val hardened = hardenTowardDifficulty(size, solution, finalRegions, finalSolveResult, targetDifficulty)
            finalRegions = hardened.regions
            finalSolveResult = hardened.solveResult
        }

        if (targetDifficulty == Difficulty.EASY) {
            val shrunk = forceOneSingleCellRegion(size, solution, repaired.regions) ?: return@repeat
            val shrunkResult = solveAnimalDoku(size, shrunk)
            if (!shrunkResult.solved) return@repeat
            finalRegions = shrunk
            finalSolveResult = shrunkResult
        }

        val difficulty = classifyAnimalDokuDifficulty(size, finalSolveResult) ?: return@repeat
        if (difficulty != targetDifficulty) return@repeat
        return AnimalDokuPuzzleEntry(
            id = "$idPrefix-${solution.joinToString("")}-${finalRegions.joinToString("") { row -> row.joinToString("") }}",
            size = size,
            difficulty = targetDifficulty.key,
            regions = finalRegions,
            solution = solution.toList(),
        )
    }
    return null
}

fun generateAnimalDokuPuzzle(
    size: Int,
    targetDifficulty: Difficulty,
    idPrefix: String,
    maxAttempts: Int = defaultMaxAttemptsFor(targetDifficulty),
): AnimalDokuPuzzleEntry? {
    repeat(maxAttempts) {
        val solution = generateSolutionPermutation(size) ?: return@repeat
        return generateAnimalDokuPuzzleForSolution(size, solution, targetDifficulty, idPrefix, maxRegionAttempts = maxAttempts) ?: return@repeat
    }
    return null
}
