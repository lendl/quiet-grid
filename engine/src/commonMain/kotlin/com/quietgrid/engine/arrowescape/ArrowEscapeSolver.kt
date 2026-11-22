package com.quietgrid.engine.arrowescape

enum class ArrowEscapeSolveTier {
    DIRECT, SINGLE_BLOCKER, CHAIN_LOOKAHEAD, BOTTLENECK, FORCED_SEQUENCE, GLOBAL_REEVALUATION
}

data class ArrowEscapeSolveMove(val pieceIndex: Int, val tier: ArrowEscapeSolveTier)

data class ArrowEscapeSolveResult(
    val solvable: Boolean,
    val highestTier: ArrowEscapeSolveTier?,
    val moves: List<ArrowEscapeSolveMove>,
    val longestChainLength: Int,
    val bottleneckCount: Int,
    val avgBranchingFactor: Double,
)

fun solveArrowEscape(
    pieces: List<ArrowEscapePiece>,
    rows: Int,
    cols: Int,
    alreadyRemoved: Set<Int> = emptySet(),
): ArrowEscapeSolveResult {
    val graph = buildDependencyGraph(pieces, rows, cols)
    val removed = alreadyRemoved.toMutableSet()
    val moves = mutableListOf<ArrowEscapeSolveMove>()
    var branchingSum = 0
    var branchingSteps = 0
    var longestChainSeen = 0

    while (removed.size < pieces.size) {
        val candidates = pieces.indices.filter { it !in removed && graph.dependsOn[it].all { dep -> dep in removed } }
        if (candidates.isEmpty()) {
            return ArrowEscapeSolveResult(
                solvable = false,
                highestTier = moves.maxByOrNull { it.tier.ordinal }?.tier,
                moves = moves,
                longestChainLength = longestChainSeen,
                bottleneckCount = moves.count { it.tier == ArrowEscapeSolveTier.BOTTLENECK },
                avgBranchingFactor = if (branchingSteps == 0) 0.0 else branchingSum.toDouble() / branchingSteps,
            )
        }

        branchingSum += candidates.size
        branchingSteps += 1

        val pick = pickTierMove(candidates, graph, removed, pieces.size)
        longestChainSeen = maxOf(longestChainSeen, pick.chainLength)
        moves.add(ArrowEscapeSolveMove(pick.pieceIndex, pick.tier))
        removed.add(pick.pieceIndex)
    }

    return ArrowEscapeSolveResult(
        solvable = true,
        highestTier = moves.maxByOrNull { it.tier.ordinal }?.tier,
        moves = moves,
        longestChainLength = longestChainSeen,
        bottleneckCount = moves.count { it.tier == ArrowEscapeSolveTier.BOTTLENECK },
        avgBranchingFactor = if (branchingSteps == 0) 0.0 else branchingSum.toDouble() / branchingSteps,
    )
}

private data class TierPick(val pieceIndex: Int, val tier: ArrowEscapeSolveTier, val chainLength: Int)

private fun pickTierMove(candidates: List<Int>, graph: DependencyGraph, removed: Set<Int>, totalPieces: Int): TierPick {
    val chainLengths = longestChainLengths(graph, removed, totalPieces)

    if (candidates.size == 1) {
        val only = candidates.single()
        return TierPick(only, ArrowEscapeSolveTier.DIRECT, chainLengths[only])
    }

    val outdegree = candidates.associateWith { index -> graph.blocks[index].count { it !in removed } }

    val bottlenecks = candidates.filter { (outdegree[it] ?: 0) >= 3 }
    if (bottlenecks.isNotEmpty()) {
        val chosen = bottlenecks.sortedWith(compareByDescending<Int> { outdegree.getValue(it) }.thenBy { it }).first()
        return TierPick(chosen, ArrowEscapeSolveTier.BOTTLENECK, chainLengths[chosen])
    }

    val chainCandidates = candidates.filter { chainLengths[it] >= 2 }
    if (chainCandidates.isNotEmpty()) {
        val chosen = chainCandidates.sortedWith(compareByDescending<Int> { chainLengths[it] }.thenBy { it }).first()
        return TierPick(chosen, ArrowEscapeSolveTier.CHAIN_LOOKAHEAD, chainLengths[chosen])
    }

    val singleBlockerCandidates = candidates.filter { (outdegree[it] ?: 0) >= 1 }
    if (singleBlockerCandidates.isNotEmpty()) {
        val chosen = singleBlockerCandidates.sortedWith(compareByDescending<Int> { outdegree.getValue(it) }.thenBy { it }).first()
        return TierPick(chosen, ArrowEscapeSolveTier.SINGLE_BLOCKER, chainLengths[chosen])
    }

    val chosen = candidates.min()
    return TierPick(chosen, ArrowEscapeSolveTier.DIRECT, chainLengths[chosen])
}

private fun longestChainLengths(graph: DependencyGraph, removed: Set<Int>, totalPieces: Int): IntArray {
    val result = IntArray(totalPieces) { -1 }
    val visiting = BooleanArray(totalPieces)

    fun dfs(node: Int): Int {
        if (result[node] >= 0) return result[node]
        if (visiting[node]) return 0
        visiting[node] = true
        var best = 0
        for (child in graph.blocks[node]) {
            if (child !in removed) best = maxOf(best, 1 + dfs(child))
        }
        visiting[node] = false
        result[node] = best
        return best
    }

    for (index in 0 until totalPieces) {
        if (index !in removed) dfs(index)
    }
    return result
}
