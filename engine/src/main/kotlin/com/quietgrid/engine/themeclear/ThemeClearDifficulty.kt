package com.quietgrid.engine.themeclear

import com.quietgrid.engine.core.Difficulty

const val THEMECLEAR_FINISH_CAP = 50
const val THEMECLEAR_MAX_COLS = 7

data class ThemeClearTierSpec(
    val letters: IntRange,
    val minWords: Int,
    val minBlindSuccess: Double,
    val maxBlindSuccess: Double,
)

val THEMECLEAR_TIERS: Map<Difficulty, ThemeClearTierSpec> = mapOf(
    Difficulty.EASY to ThemeClearTierSpec(letters = 12..16, minWords = 3, minBlindSuccess = 0.20, maxBlindSuccess = 1.0),
    Difficulty.MEDIUM to ThemeClearTierSpec(letters = 20..25, minWords = 4, minBlindSuccess = 0.02, maxBlindSuccess = 0.20),
    Difficulty.HARD to ThemeClearTierSpec(letters = 28..36, minWords = 5, minBlindSuccess = 0.0, maxBlindSuccess = 0.20),
    Difficulty.EXPERT to ThemeClearTierSpec(letters = 40..48, minWords = 5, minBlindSuccess = 0.0, maxBlindSuccess = 0.20),
)

private fun ThemeClearTierSpec.acceptsBlindPlay(letterCount: Int, blindSuccessRate: Double): Boolean =
    letterCount in letters && blindSuccessRate >= minBlindSuccess && blindSuccessRate <= maxBlindSuccess

fun themeClearMeetsTier(difficulty: Difficulty, letterCount: Int, metrics: ThemeClearMetrics): Boolean =
    THEMECLEAR_TIERS.getValue(difficulty).acceptsBlindPlay(letterCount, metrics.blindSuccessRate) && metrics.finishCount >= 1

fun themeClearMeetsTier(difficulty: Difficulty, letters: String, solver: ThemeClearSolver): Boolean {
    val spec = THEMECLEAR_TIERS.getValue(difficulty)
    if (letters.length !in spec.letters) return false
    return spec.acceptsBlindPlay(letters.length, solver.blindSuccessRate(letters)) && solver.findFinish(letters) != null
}
