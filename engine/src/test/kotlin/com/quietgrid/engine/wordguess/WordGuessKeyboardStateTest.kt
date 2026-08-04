package com.quietgrid.engine.wordguess

import org.junit.Assert.assertEquals
import org.junit.Test

class WordGuessKeyboardStateTest {
    @Test
    fun `foldWordGuessKeyboardState folds a single guess's feedback per letter`() {
        val guesses = listOf("cat" to evaluateGuess(target = "dog", guess = "cat"))
        val state = foldWordGuessKeyboardState(guesses)
        assertEquals(LetterState.ABSENT, state['c'])
        assertEquals(LetterState.ABSENT, state['a'])
        assertEquals(LetterState.ABSENT, state['t'])
    }

    @Test
    fun `foldWordGuessKeyboardState upgrades a letter from PRESENT to CORRECT across guesses`() {
        // First guess: 'd' is present-elsewhere (target "dog", guess "bad" -> d at index2 not index0).
        // Second guess: 'd' lands in the correct spot.
        val guesses = listOf(
            "bad" to evaluateGuess(target = "dog", guess = "bad"),
            "dog" to evaluateGuess(target = "dog", guess = "dog"),
        )
        val state = foldWordGuessKeyboardState(guesses)
        assertEquals(LetterState.CORRECT, state['d'])
    }

    @Test
    fun `foldWordGuessKeyboardState never downgrades a letter from CORRECT back to PRESENT or ABSENT`() {
        val guesses = listOf(
            "dog" to evaluateGuess(target = "dog", guess = "dog"),
            "bad" to evaluateGuess(target = "dog", guess = "bad"),
        )
        val state = foldWordGuessKeyboardState(guesses)
        assertEquals(LetterState.CORRECT, state['d'])
    }
}
