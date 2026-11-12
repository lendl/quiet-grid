package com.quietgrid.engine.circuit

import com.quietgrid.engine.core.Difficulty

const val CIRCUIT_LONG_WHAT_IF_CHAIN = 2

data class CircuitSolveProfile(
    val loopSteps: Int,
    val deadEndSteps: Int,
    val contradictionSteps: Int,
    val longWhatIfSteps: Int,
    val maxChainLength: Int,
)

fun analyzeCircuitSolveResult(result: CircuitSolveResult): CircuitSolveProfile {
    val contradictions = result.steps.filter { it.technique == CircuitTechnique.CONTRADICTION }
    return CircuitSolveProfile(
        loopSteps = result.steps.count { it.technique == CircuitTechnique.NO_LOOP },
        deadEndSteps = result.steps.count { it.technique == CircuitTechnique.DEAD_END },
        contradictionSteps = contradictions.size,
        longWhatIfSteps = contradictions.count { it.chainLength >= CIRCUIT_LONG_WHAT_IF_CHAIN },
        maxChainLength = contradictions.maxOfOrNull { it.chainLength } ?: 0,
    )
}

data class CircuitTier(
    val rows: Int,
    val cols: Int,
    val wrap: Boolean,
    val longWhatIfs: IntRange,
)

val CIRCUIT_TIERS: Map<Difficulty, CircuitTier> = mapOf(
    Difficulty.EASY to CircuitTier(5, 5, wrap = false, longWhatIfs = 0..0),
    Difficulty.MEDIUM to CircuitTier(7, 7, wrap = false, longWhatIfs = 1..2),
    Difficulty.HARD to CircuitTier(7, 7, wrap = true, longWhatIfs = 3..9),
    Difficulty.EXPERT to CircuitTier(8, 8, wrap = true, longWhatIfs = 12..40),
)

fun circuitTierDistance(tier: CircuitTier, profile: CircuitSolveProfile): Int {
    val count = profile.longWhatIfSteps
    val below = maxOf(0, tier.longWhatIfs.first - count)
    val above = maxOf(0, count - tier.longWhatIfs.last)
    return below + above
}

fun classifyCircuitDifficulty(board: CircuitBoard, result: CircuitSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeCircuitSolveResult(result)
    return CIRCUIT_TIERS.entries.firstOrNull { (_, tier) ->
        tier.rows == board.rows && tier.cols == board.cols && tier.wrap == board.wrap && circuitTierDistance(tier, profile) == 0
    }?.key
}
