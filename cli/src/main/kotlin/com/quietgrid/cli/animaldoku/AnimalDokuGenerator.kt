package com.quietgrid.cli.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuSolveResult
import com.quietgrid.engine.animaldoku.classifyAnimalDokuDifficulty
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import com.quietgrid.engine.core.Difficulty
import kotlin.math.abs

/**
 * Random permutation (index = row, value = column) with no two consecutive rows' columns within 1
 * of each other — the only constraint needed to guarantee no two placed animals are king-adjacent,
 * since non-consecutive rows can never be within adjacency distance regardless of column.
 */
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

/**
 * Grows size regions via randomized simultaneous flood-fill, one seeded per row at its solution
 * cell (region id = row index), until every cell is claimed. Naturally produces a random mix of
 * compact and sprawling connected shapes across repeated calls — difficulty tuning happens by
 * generating many candidates and keeping only ones the solver classifies into the target tier
 * (see [generateAnimalDokuPuzzle]), not by directly steering shape here.
 */
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
        // Every unclaimed cell always has some claimed 4-neighbor on a fully-connected grid grown
        // from live seeds; a stuck pass here would indicate a real bug, not an expected retry path.
        if (!progressed) return null
    }
    return regionOf.map { it.toList() }
}

data class AnimalDokuRepairedPuzzle(val regions: List<List<Int>>, val solveResult: AnimalDokuSolveResult)

/**
 * Repairs a region layout toward a uniquely-solvable state via local search: when solving stalls,
 * mutate one non-solution boundary cell (move it from its current region to an adjacent region)
 * and retry solving from the mutated layout, rather than discarding the whole layout and
 * regenerating from scratch. Never moves a cell that's part of [solution] — this guarantees the
 * seeded solution remains a valid assignment for the mutated regions throughout (the solution
 * permutation's cells never change region ownership, so it's always still "one per region").
 */
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

/**
 * True if removing [excluded] from [region] leaves the region's remaining cells still a single
 * 4-connected component (i.e. the region survives losing that one cell without splitting apart).
 */
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

/**
 * A region must hold at least this many cells to be a *preferred* donor. At 3, a donor still has
 * 2+ cells after giving one away, so preferred mutations can never strand a region at exactly one
 * cell. A 1-cell region is a free giveaway — its animal is knowable with zero reasoning the
 * instant the puzzle starts — and unbiased repair drifted into that shape constantly (6 of 9
 * generated puzzles in the first real CLI run contained one).
 */
internal const val MIN_DONOR_REGION_SIZE_TO_PREFER = 3

/**
 * All valid single-cell region reassignments: a non-solution cell, orthogonally adjacent to a
 * different region, whose donor region stays connected after losing it (the recipient region
 * trivially stays connected since it's gaining a cell adjacent to itself).
 *
 * Biased away from shrinking already-tiny regions: candidates whose donor holds at least
 * [minDonorRegionSizeToPrefer] cells are returned alone whenever any exist, and the unrestricted
 * pool is returned only as a fallback. That keeps the bias soft — the search can still make any
 * legal move when no balanced one is available, so completeness (and thus the repair loop's
 * ability to escape a corner) is preserved.
 */
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
            // Connectivity depends only on the cell being removed, not on which neighbor receives
            // it, so run the BFS once per cell rather than once per direction.
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

/**
 * Picks one random reassignment from [boundaryMutationCandidates] and applies it. Returns null if
 * no valid reassignment exists (should be rare on a healthy layout; the caller's repeat loop just
 * tries again on the same regions next iteration).
 */
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

/**
 * All valid single-cell donations *from* [donorRegion] specifically: a non-solution cell inside
 * that region, orthogonally adjacent to a different region, whose donor region stays connected
 * after losing it. This is [boundaryMutationCandidates]'s same connectivity check
 * ([isConnectedWithoutCell]), scoped to one fixed donor so a specific region can be deliberately
 * drained rather than mutated at random.
 */
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
            // Same rule as boundaryMutationCandidates: the donor region must survive losing this
            // cell without splitting into two pieces.
            if (!isConnectedWithoutCell(size, regionOf, donorRegion, row to col)) continue
            for (neighborRegion in neighborRegions) {
                candidates.add(Triple(row, col, neighborRegion))
            }
        }
    }
    return candidates
}

/**
 * Shrinks one region -- the smallest in [regions], ties broken by lowest region id -- down to
 * just its own solution cell, by repeatedly donating its non-solution cells to adjacent regions.
 * Used to guarantee EASY puzzles a "freebie" region: a single cell whose animal is knowable with
 * zero reasoning, giving the solver a bootstrap point.
 *
 * Reuses the exact connectivity-preserving donation logic already used for general repair
 * ([isConnectedWithoutCell] via [donationCandidatesFrom]), just aimed at one fixed target region
 * instead of picking donations at random across the whole grid. The solution cell of the target
 * region is never touched (donations only ever consider non-solution cells).
 *
 * Returns null if the region can't be fully drained to one cell -- e.g. a donation exists that
 * would strand the region at 2 cells but no further legal donation exists from there. Callers
 * should treat a null result as a failed attempt and retry with a fresh layout rather than
 * shipping a partially-shrunk region as though it were the guaranteed single-cell freebie.
 */
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

/**
 * How many local boundary-cell mutations [repairRegionsTowardUniqueSolution] gets before giving up
 * on an initial region layout. The function's own default (50) is far too small for size 7-8: a
 * gate-counter diagnostic showed it failing to reach ANY unique solution on 497-500 of 500 raw
 * attempts at those sizes, while 400 recovered a real (non-zero) success rate at both -- larger
 * grids simply need more local search room to untangle into a uniquely-solvable layout. Applied to
 * every size uniformly since a larger cap is harmless for small grids (already-successful attempts
 * return immediately; the higher ceiling only ever gets exercised by attempts that would otherwise
 * have failed).
 */
private const val REPAIR_ATTEMPTS_FOR_GENERATION = 400

/**
 * How many fresh solution+region+repair seeds [generateAnimalDokuPuzzle] tries before giving up, per
 * target difficulty. EASY/MEDIUM keep the original generous default (300) since they converge in a
 * handful of attempts regardless. HARD and EXPERT are cut down sharply: since the hardening phase
 * below now does the bulk of the real search work per seed (up to thousands of mutations), each seed
 * carries far more search power than a bare repair attempt did, so far fewer seeds are needed -- and
 * trying hundreds of them at the old default would be needlessly slow. EXPERT gets the smaller of
 * the two budgets since its hardening climb is open-ended (each seed can spend much longer hardening
 * than HARD's ceiling-bounded climb typically does).
 *
 * `internal` (not `private`) so `AnimalDokuGeneratorTest` can assert on it directly, matching this
 * file's existing convention for tested-but-not-public-API helpers (`isConnectedWithoutCell`,
 * `boundaryMutationCandidates`, `mutateOneBoundaryCell`, `donationCandidatesFrom`).
 */
internal fun defaultMaxAttemptsFor(targetDifficulty: Difficulty): Int = when (targetDifficulty) {
    Difficulty.HARD -> 50
    Difficulty.EXPERT -> 20
    else -> 300
}

/** Generates a single AnimalDoku puzzle at [size]/[targetDifficulty], retrying internally on failed attempts. */
fun generateAnimalDokuPuzzle(
    size: Int,
    targetDifficulty: Difficulty,
    idPrefix: String,
    maxAttempts: Int = defaultMaxAttemptsFor(targetDifficulty),
): AnimalDokuPuzzleEntry? {
    repeat(maxAttempts) {
        val solution = generateSolutionPermutation(size) ?: return@repeat
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
            // Force a guaranteed single-cell "freebie" region, then fully re-verify: shrinking a
            // region changes the puzzle, so it must be re-solved and re-classified as EASY under
            // the new layout before this attempt is accepted.
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
