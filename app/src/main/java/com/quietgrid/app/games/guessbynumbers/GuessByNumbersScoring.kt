package com.quietgrid.app.games.guessbynumbers

import kotlin.math.max

private val GUESS_BY_NUMBERS_TIER_BASE = mapOf(
    "easy" to 8_000,
    "medium" to 10_000,
    "hard" to 12_000,
    "expert" to 15_000,
)
private const val GUESS_BY_NUMBERS_GUESS_PENALTY = 480
private const val GUESS_BY_NUMBERS_TIME_PENALTY_PER_SECOND = 20

fun computeGuessByNumbersScore(difficultyKey: String, guessesUsed: Int, elapsedSeconds: Int): Int {
    val base = GUESS_BY_NUMBERS_TIER_BASE[difficultyKey] ?: 8_000
    val penalty = (guessesUsed - 1) * GUESS_BY_NUMBERS_GUESS_PENALTY + elapsedSeconds * GUESS_BY_NUMBERS_TIME_PENALTY_PER_SECOND
    return max(0, base - penalty)
}
