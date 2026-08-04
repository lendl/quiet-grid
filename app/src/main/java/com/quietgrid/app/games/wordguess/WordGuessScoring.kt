package com.quietgrid.app.games.wordguess

import kotlin.math.max

private val WORDGUESS_TIER_BASE = mapOf(
    "easy" to 8_000,
    "medium" to 10_000,
    "hard" to 12_000,
    "expert" to 15_000,
)
private const val WORDGUESS_GUESS_PENALTY = 800
private const val WORDGUESS_TIME_PENALTY_PER_SECOND = 20

fun computeWordGuessScore(difficultyKey: String, guessesUsed: Int, elapsedSeconds: Int): Int {
    val base = WORDGUESS_TIER_BASE[difficultyKey] ?: 8_000
    val penalty = (guessesUsed - 1) * WORDGUESS_GUESS_PENALTY + elapsedSeconds * WORDGUESS_TIME_PENALTY_PER_SECOND
    return max(0, base - penalty)
}
