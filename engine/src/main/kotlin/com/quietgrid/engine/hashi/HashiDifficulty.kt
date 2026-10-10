package com.quietgrid.engine.hashi

import com.quietgrid.engine.core.Difficulty

data class HashiSolveProfile(
    val isolationSteps: Int,
    val contradictionSteps: Int,
    val maxChainDepth: Int,
)

fun analyzeHashiSolveResult(result: HashiSolveResult): HashiSolveProfile {
    val contradictions = result.steps.filter { it.technique == HashiTechnique.CONTRADICTION }
    return HashiSolveProfile(
        isolationSteps = result.steps.count { it.technique == HashiTechnique.ISOLATION },
        contradictionSteps = contradictions.size,
        maxChainDepth = contradictions.maxOfOrNull { it.chainDepth } ?: 0,
    )
}

const val HASHI_MEDIUM_ISOLATION_FLOOR = 2
const val HASHI_EXPERT_CONTRADICTION_FLOOR = 4

fun classifyHashiDifficulty(result: HashiSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeHashiSolveResult(result)
    return when {
        profile.contradictionSteps >= HASHI_EXPERT_CONTRADICTION_FLOOR -> Difficulty.EXPERT
        profile.contradictionSteps > 0 -> Difficulty.HARD
        profile.isolationSteps >= HASHI_MEDIUM_ISOLATION_FLOOR -> Difficulty.MEDIUM
        else -> Difficulty.EASY
    }
}
