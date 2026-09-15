package com.quietgrid.engine.starbattle

const val STARBATTLE_MEDIUM_K2_CHAIN_DEPTH_FLOOR = 1
const val STARBATTLE_HARD_K2_CHAIN_DEPTH_FLOOR = 3
const val STARBATTLE_EXPERT_CHAIN_DEPTH_FLOOR = 5
const val STARBATTLE_EXPERT_CHAIN_REPEATS_FLOOR = 5

data class StarBattleSolveProfile(
    val hardestTechnique: StarBattleTechnique,
    val hardestTechniqueRepeats: Int,
    val maxChainDepth: Int,
    val chainRepeats: Int,
    val confinementRepeats: Int,
)

fun analyzeStarBattleSolveResult(result: StarBattleSolveResult): StarBattleSolveProfile {
    val hardestTechnique = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: StarBattleTechnique.FORCED_PLACEMENT
    val chainSteps = result.steps.filter { it.technique == StarBattleTechnique.CHAIN }
    val maxChainDepth = chainSteps.maxOfOrNull { it.chainDepth } ?: 0
    val hardestTechniqueRepeats = result.steps.count { it.technique == hardestTechnique }
    val confinementRepeats = result.steps.count { it.technique == StarBattleTechnique.CONFINEMENT }
    return StarBattleSolveProfile(hardestTechnique, hardestTechniqueRepeats, maxChainDepth, chainSteps.size, confinementRepeats)
}

fun classifyStarBattleK2Grade(result: StarBattleSolveResult): String? {
    if (!result.solved) return null
    val profile = analyzeStarBattleSolveResult(result)
    if (profile.chainRepeats == 0) return null
    return when {
        profile.maxChainDepth >= STARBATTLE_EXPERT_CHAIN_DEPTH_FLOOR || profile.chainRepeats >= STARBATTLE_EXPERT_CHAIN_REPEATS_FLOOR -> "expert"
        profile.maxChainDepth >= STARBATTLE_HARD_K2_CHAIN_DEPTH_FLOOR -> "hard"
        profile.maxChainDepth >= STARBATTLE_MEDIUM_K2_CHAIN_DEPTH_FLOOR -> "medium"
        else -> null
    }
}
