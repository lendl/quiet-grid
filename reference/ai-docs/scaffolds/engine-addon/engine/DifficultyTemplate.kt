package com.quietgrid.engine.__game__

// __Game__Difficulty.kt template — classify a solved puzzle into the four-tier ladder using move evidence
// from __Game__Moves.kt. See reference/ai-docs/context/difficulties.md for validation expectations.

data class __Game__DifficultyEvidence(
    val moveCount: Int,
    val advancedMoveCount: Int,
    val guessCount: Int,
)

fun classifyDifficulty(evidence: __Game__DifficultyEvidence): __Game__Difficulty? {
    if (evidence.guessCount > 0) return null
    return when {
        evidence.advancedMoveCount >= 8 -> __Game__Difficulty.EXPERT
        evidence.advancedMoveCount >= 4 -> __Game__Difficulty.HARD
        evidence.moveCount >= 6 -> __Game__Difficulty.MEDIUM
        else -> __Game__Difficulty.EASY
    }
}
