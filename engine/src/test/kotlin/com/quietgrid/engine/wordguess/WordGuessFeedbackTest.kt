package com.quietgrid.engine.wordguess

import org.junit.Assert.assertEquals
import org.junit.Test

class WordGuessFeedbackTest {
    @Test
    fun `evaluateGuess marks every letter CORRECT on an exact match`() {
        assertEquals(
            List(5) { LetterState.CORRECT },
            evaluateGuess(target = "apple", guess = "apple"),
        )
    }

    @Test
    fun `evaluateGuess marks every letter ABSENT when no letters overlap`() {
        assertEquals(
            List(5) { LetterState.ABSENT },
            evaluateGuess(target = "apple", guess = "birch"),
        )
    }

    @Test
    fun `evaluateGuess handles duplicate letters without double-counting`() {
        val result = evaluateGuess(target = "apple", guess = "paper")
        assertEquals(
            listOf(
                LetterState.PRESENT,
                LetterState.PRESENT,
                LetterState.CORRECT,
                LetterState.PRESENT,
                LetterState.ABSENT,
            ),
            result,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `evaluateGuess rejects mismatched lengths`() {
        evaluateGuess(target = "apple", guess = "ape")
    }
}
