package com.quietgrid.cli.wordguess

import com.quietgrid.cli.GenerationState
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry

fun generateWordGuessAnswerEntries(
    locale: String,
    difficulty: Difficulty,
    tiers5: WordGuessTieredWords,
    tiers6: WordGuessTieredWords,
    count: Int,
    state: GenerationState,
): List<WordGuessPuzzleEntry> {
    val pool = when (difficulty) {
        Difficulty.EASY -> tiers5.common
        Difficulty.MEDIUM -> tiers5.full
        Difficulty.HARD -> tiers6.common
        Difficulty.EXPERT -> tiers6.full
    }

    return pool
        .asSequence()
        .filter { word -> !state.hasTried("$locale:${difficulty.key}:$word") }
        .take(count)
        .map { word ->
            state.recordTried("$locale:${difficulty.key}:$word", "valid")
            WordGuessPuzzleEntry(id = "$locale-${difficulty.key}-$word", locale = locale, difficulty = difficulty.key, word = word)
        }
        .toList()
}
