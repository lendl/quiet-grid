package com.quietgrid.engine.wordguess

enum class LetterState { CORRECT, PRESENT, ABSENT }

fun evaluateGuess(target: String, guess: String): List<LetterState> {
    require(target.length == guess.length) { "Guess length (${guess.length}) must match target length (${target.length})" }

    val result = MutableList(guess.length) { LetterState.ABSENT }
    val remaining = mutableMapOf<Char, Int>()

    for (i in guess.indices) {
        if (guess[i] == target[i]) {
            result[i] = LetterState.CORRECT
        } else {
            remaining[target[i]] = (remaining[target[i]] ?: 0) + 1
        }
    }

    for (i in guess.indices) {
        if (result[i] == LetterState.CORRECT) continue
        val ch = guess[i]
        val count = remaining[ch] ?: 0
        if (count > 0) {
            result[i] = LetterState.PRESENT
            remaining[ch] = count - 1
        }
    }

    return result
}
