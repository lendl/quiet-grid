package com.quietgrid.engine.flowfree

import com.quietgrid.engine.core.Difficulty

const val FLOWFREE_SIZE = 7

val FLOWFREE_PAIR_COUNT_RANGE: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 8..9,
    Difficulty.MEDIUM to 6..7,
    Difficulty.HARD to 5..6,
    Difficulty.EXPERT to 4..5,
)

data class FlowFreeSolveProfile(
    val peakTechnique: FlowFreeTechnique,
    val branchSearchCount: Int,
    val regionIsolationCount: Int,
    val maxBranchWidth: Int,
)

fun analyzeFlowFreeSolveResult(steps: List<FlowFreeSolveStep>): FlowFreeSolveProfile {
    val peak = steps.maxByOrNull { it.technique.ordinal }?.technique ?: FlowFreeTechnique.FORCED_CONTINUATION
    val branchSteps = steps.filter { it.technique == FlowFreeTechnique.BRANCH_SEARCH }
    val isolationCount = steps.count { it.technique == FlowFreeTechnique.REGION_ISOLATION }
    val maxBranchWidth = branchSteps.maxOfOrNull { it.branchWidth } ?: 0
    return FlowFreeSolveProfile(peak, branchSteps.size, isolationCount, maxBranchWidth)
}

fun classifyFlowFreeDifficulty(result: FlowFreeSolveResult): String? {
    if (!result.solved) return null
    val profile = analyzeFlowFreeSolveResult(result.steps)
    return when {
        profile.branchSearchCount >= 9 || profile.maxBranchWidth >= 4 -> "expert"
        profile.branchSearchCount >= 6 -> "hard"
        profile.branchSearchCount >= 4 -> "medium"
        else -> "easy"
    }
}
