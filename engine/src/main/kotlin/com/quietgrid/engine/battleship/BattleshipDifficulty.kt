package com.quietgrid.engine.battleship

import com.quietgrid.engine.core.Difficulty

val BATTLESHIP_FLEET_EASY = listOf(1, 2, 3)
val BATTLESHIP_FLEET_MEDIUM = listOf(1, 2, 3, 4)
val BATTLESHIP_FLEET_HARD_EXPERT = listOf(1, 2, 3, 4, 5)

val BATTLESHIP_SIZE_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 6,
    Difficulty.MEDIUM to 8,
    Difficulty.HARD to 10,
    Difficulty.EXPERT to 10,
)

val BATTLESHIP_FLEET_BY_DIFFICULTY: Map<Difficulty, List<Int>> = mapOf(
    Difficulty.EASY to BATTLESHIP_FLEET_EASY,
    Difficulty.MEDIUM to BATTLESHIP_FLEET_MEDIUM,
    Difficulty.HARD to BATTLESHIP_FLEET_HARD_EXPERT,
    Difficulty.EXPERT to BATTLESHIP_FLEET_HARD_EXPERT,
)

data class BattleshipSolveProfile(
    val hardestTechnique: BattleshipTechnique,
    val fleetLengthMatchCount: Int,
    val fleetEliminationCount: Int,
    val probingCount: Int,
)

fun analyzeBattleshipSolveResult(result: BattleshipSolveResult): BattleshipSolveProfile {
    val hardest = result.steps.maxByOrNull { it.technique.ordinal }?.technique ?: BattleshipTechnique.STRUCTURAL_WATER
    return BattleshipSolveProfile(
        hardestTechnique = hardest,
        fleetLengthMatchCount = result.steps.count { it.technique == BattleshipTechnique.FLEET_LENGTH_MATCH },
        fleetEliminationCount = result.steps.count { it.technique == BattleshipTechnique.FLEET_ELIMINATION },
        probingCount = result.steps.count { it.technique == BattleshipTechnique.PROBING },
    )
}

fun classifyBattleshipDifficulty(result: BattleshipSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeBattleshipSolveResult(result)
    return when {
        profile.hardestTechnique.ordinal <= BattleshipTechnique.LINE_EXHAUSTION.ordinal -> Difficulty.EASY
        profile.probingCount <= 2 -> Difficulty.MEDIUM
        profile.probingCount <= 6 -> Difficulty.HARD
        else -> Difficulty.EXPERT
    }
}
