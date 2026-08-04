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
        // target "apple" has P at index 1 and 2; guess "paper" has P at index 0 and 2.
        // index 2 matches exactly (CORRECT), consuming one of target's two P's directly.
        // the other target P (index 1, unmatched) is claimed by guess's other P (index 0) as PRESENT.
        val result = evaluateGuess(target = "apple", guess = "paper")
        assertEquals(
            listOf(
                LetterState.PRESENT, // guess[0]='p' -> present (claims target's leftover P)
                LetterState.PRESENT, // guess[1]='a' -> present (target has 'a' at index 0)
                LetterState.CORRECT, // guess[2]='p' -> correct (target[2]='p')
                LetterState.PRESENT, // guess[3]='e' -> present (target has 'e' at index 4)
                LetterState.ABSENT,  // guess[4]='r' -> absent (target has no 'r')
            ),
            result,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `evaluateGuess rejects mismatched lengths`() {
        evaluateGuess(target = "apple", guess = "ape")
    }
}
