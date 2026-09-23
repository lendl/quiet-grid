package com.quietgrid.cli.flowfree

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import com.quietgrid.engine.flowfree.classifyFlowFreeDifficulty
import com.quietgrid.engine.flowfree.countFlowFreeSolutions
import com.quietgrid.engine.flowfree.endpointsOf
import com.quietgrid.engine.flowfree.generateFlowFreeSolution
import com.quietgrid.engine.flowfree.solveFlowFree
import kotlin.random.Random

fun generateFlowFreePuzzleForTier(
    size: Int,
    pairCount: Int,
    targetTier: Difficulty,
    idPrefix: String,
    random: Random = Random.Default,
): FlowFreePuzzleEntry? {
    val solutionPaths = generateFlowFreeSolution(size, pairCount, random) ?: return null
    val endpoints = endpointsOf(size, pairCount, solutionPaths)

    val starts = mutableListOf<Pair<Int, Int>>()
    val targets = mutableListOf<Pair<Int, Int>>()
    for (color in 0 until pairCount) {
        val cells = (0 until size).flatMap { r -> (0 until size).mapNotNull { c -> if (endpoints[r][c] == color) r to c else null } }
        if (cells.size != 2) return null
        starts += cells[0]
        targets += cells[1]
    }

    val result = solveFlowFree(size, pairCount, starts, targets)
    if (!result.solved) return null
    if (classifyFlowFreeDifficulty(result) != targetTier.key) return null
    if (countFlowFreeSolutions(size, pairCount, starts, targets, cap = 2, maxNodes = 5000000) != 1) return null

    return FlowFreePuzzleEntry(
        id = "$idPrefix-${solutionPaths.joinToString("|") { path -> path.joinToString(",") }}",
        size = size,
        difficulty = targetTier.key,
        pairCount = pairCount,
        endpoints = endpoints,
        paths = solutionPaths,
    )
}
