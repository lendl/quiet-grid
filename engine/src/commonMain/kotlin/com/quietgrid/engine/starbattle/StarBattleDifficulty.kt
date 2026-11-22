package com.quietgrid.engine.starbattle

const val STARBATTLE_MEDIUM_K2_CHAIN_DEPTH_FLOOR = 1
const val STARBATTLE_K2_MIN_REGION_SIZE = 4

const val STARBATTLE_HARD_K2_MIN_CHAINS = 6
const val STARBATTLE_HARD_K2_MIN_STEPS_BEFORE_FIRST_PLACEMENT = 6

const val STARBATTLE_EXPERT_K2_MIN_CHAINS = 12
const val STARBATTLE_EXPERT_K2_MIN_CHAIN_DEPTH = 8
const val STARBATTLE_EXPERT_K2_MIN_STEPS_BEFORE_FIRST_PLACEMENT = 12

val STARBATTLE_K2_SIZES_BY_GRADE: Map<String, List<Int>> = mapOf(
    "medium" to listOf(8),
    "hard" to listOf(8, 9),
    "expert" to listOf(9, 10),
)

data class StarBattleSolveProfile(
    val hardestTechnique: StarBattleTechnique,
    val hardestTechniqueRepeats: Int,
    val maxChainDepth: Int,
    val chainRepeats: Int,
    val confinementRepeats: Int,
    val stepsBeforeFirstPlacement: Int,
)

fun analyzeStarBattleSolveResult(result: StarBattleSolveResult): StarBattleSolveProfile {
    val hardestTechnique = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: StarBattleTechnique.FORCED_PLACEMENT
    val chainSteps = result.steps.filter { it.technique == StarBattleTechnique.CHAIN }
    val maxChainDepth = chainSteps.maxOfOrNull { it.chainDepth } ?: 0
    val hardestTechniqueRepeats = result.steps.count { it.technique == hardestTechnique }
    val confinementRepeats = result.steps.count { it.technique == StarBattleTechnique.CONFINEMENT }
    val firstPlacement = result.steps.indexOfFirst { it.technique == StarBattleTechnique.FORCED_PLACEMENT }
    val stepsBeforeFirstPlacement = if (firstPlacement == -1) result.steps.size else firstPlacement
    return StarBattleSolveProfile(
        hardestTechnique,
        hardestTechniqueRepeats,
        maxChainDepth,
        chainSteps.size,
        confinementRepeats,
        stepsBeforeFirstPlacement,
    )
}

fun classifyStarBattleK2Grade(result: StarBattleSolveResult): String? {
    if (!result.solved) return null
    val profile = analyzeStarBattleSolveResult(result)
    if (profile.chainRepeats == 0) return null
    return when {
        profile.chainRepeats >= STARBATTLE_EXPERT_K2_MIN_CHAINS &&
            profile.maxChainDepth >= STARBATTLE_EXPERT_K2_MIN_CHAIN_DEPTH &&
            profile.stepsBeforeFirstPlacement >= STARBATTLE_EXPERT_K2_MIN_STEPS_BEFORE_FIRST_PLACEMENT -> "expert"
        profile.chainRepeats >= STARBATTLE_HARD_K2_MIN_CHAINS &&
            profile.stepsBeforeFirstPlacement >= STARBATTLE_HARD_K2_MIN_STEPS_BEFORE_FIRST_PLACEMENT -> "hard"
        profile.maxChainDepth >= STARBATTLE_MEDIUM_K2_CHAIN_DEPTH_FLOOR -> "medium"
        else -> null
    }
}

fun meetsStarBattleK2RegionShape(grade: String, size: Int, regions: List<List<Int>>): Boolean {
    val sizes = IntArray(size)
    for (row in regions) for (region in row) sizes[region]++
    return grade == "medium" || sizes.min() >= STARBATTLE_K2_MIN_REGION_SIZE
}

fun classifyStarBattleK2Puzzle(size: Int, regions: List<List<Int>>, result: StarBattleSolveResult): String? {
    val grade = classifyStarBattleK2Grade(result) ?: return null
    if (size !in STARBATTLE_K2_SIZES_BY_GRADE.getValue(grade)) return null
    return if (meetsStarBattleK2RegionShape(grade, size, regions)) grade else null
}
