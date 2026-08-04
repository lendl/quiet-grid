package com.quietgrid.engine.wordguess

private fun rank(state: LetterState): Int = when (state) {
    LetterState.ABSENT -> 0
    LetterState.PRESENT -> 1
    LetterState.CORRECT -> 2
}

fun foldWordGuessKeyboardState(guesses: List<Pair<String, List<LetterState>>>): Map<Char, LetterState> {
    val state = mutableMapOf<Char, LetterState>()
    for ((guess, feedback) in guesses) {
        guess.forEachIndexed { index, ch ->
            val next = feedback[index]
            val current = state[ch]
            if (current == null || rank(next) > rank(current)) {
                state[ch] = next
            }
        }
    }
    return state
}
