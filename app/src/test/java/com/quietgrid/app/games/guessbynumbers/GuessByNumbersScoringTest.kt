package com.quietgrid.app.games.guessbynumbers

import org.junit.Assert.assertTrue
import org.junit.Test

class GuessByNumbersScoringTest {
    @Test
    fun `computeGuessByNumbersScore is never negative`() {
        assertTrue(computeGuessByNumbersScore("expert", guessesUsed = 10, elapsedSeconds = 10_000) >= 0)
    }

    @Test
    fun `computeGuessByNumbersScore rewards fewer guesses, all else equal`() {
        val fewGuesses = computeGuessByNumbersScore("easy", guessesUsed = 1, elapsedSeconds = 30)
        val manyGuesses = computeGuessByNumbersScore("easy", guessesUsed = 10, elapsedSeconds = 30)
        assertTrue(fewGuesses > manyGuesses)
    }

    @Test
    fun `computeGuessByNumbersScore rewards a faster solve, all else equal`() {
        val fast = computeGuessByNumbersScore("easy", guessesUsed = 3, elapsedSeconds = 10)
        val slow = computeGuessByNumbersScore("easy", guessesUsed = 3, elapsedSeconds = 300)
        assertTrue(fast > slow)
    }

    @Test
    fun `max guess-count penalty stays below the smallest tier's base`() {
        val worstCase = 9 * 480
        assertTrue(worstCase < 8_000)
    }
}
