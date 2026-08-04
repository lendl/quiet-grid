package com.quietgrid.app.games.wordguess

import org.junit.Assert.assertTrue
import org.junit.Test

class WordGuessScoringTest {
    @Test
    fun `computeWordGuessScore is never negative`() {
        assertTrue(computeWordGuessScore("expert", guessesUsed = 6, elapsedSeconds = 10_000) >= 0)
    }

    @Test
    fun `computeWordGuessScore rewards fewer guesses, all else equal`() {
        val fewGuesses = computeWordGuessScore("easy", guessesUsed = 1, elapsedSeconds = 30)
        val manyGuesses = computeWordGuessScore("easy", guessesUsed = 6, elapsedSeconds = 30)
        assertTrue(fewGuesses > manyGuesses)
    }

    @Test
    fun `computeWordGuessScore rewards a faster solve, all else equal`() {
        val fast = computeWordGuessScore("easy", guessesUsed = 3, elapsedSeconds = 10)
        val slow = computeWordGuessScore("easy", guessesUsed = 3, elapsedSeconds = 300)
        assertTrue(fast > slow)
    }
}
