package com.quietgrid.app.games.guessbynumbers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuessByNumbersLogicTest {
    private val dictionary = setOf("apple", "grape", "mango", "zzzzz", "brown", "retro", "wrong")

    private fun freshSession(target: String = "apple") = GuessByNumbersSession(
        puzzleId = "en-easy-apple",
        locale = "en",
        difficulty = "easy",
        targetWord = target,
        wordLength = target.length,
        guesses = emptyList(),
        status = GuessByNumbersStatus.PLAYING,
    )

    @Test
    fun `computeMatchesAndExact matches the RETRO example against BROWN`() {
        val (matches, exact) = computeMatchesAndExact("brown", "retro")
        assertEquals(2, matches)
        assertEquals(0, exact)
    }

    @Test
    fun `computeMatchesAndExact matches the WRONG example against BROWN`() {
        val (matches, exact) = computeMatchesAndExact("brown", "wrong")
        assertEquals(4, matches)
        assertEquals(2, exact)
    }

    @Test
    fun `submitGuessByNumbersGuess rejects a guess not in the dictionary, leaving the session untouched`() {
        val session = freshSession()
        val result = submitGuessByNumbersGuess(session, dictionary, "qqqqq")
        assertTrue(result is GuessByNumbersSubmitResult.InvalidWord)
    }

    @Test
    fun `submitGuessByNumbersGuess appends a valid guess and stays PLAYING when wrong`() {
        val session = freshSession()
        val result = submitGuessByNumbersGuess(session, dictionary, "grape") as GuessByNumbersSubmitResult.Updated
        assertEquals(1, result.session.guesses.size)
        assertEquals(GuessByNumbersStatus.PLAYING, result.session.status)
    }

    @Test
    fun `submitGuessByNumbersGuess marks the session WON on an exact match`() {
        val session = freshSession()
        val result = submitGuessByNumbersGuess(session, dictionary, "apple") as GuessByNumbersSubmitResult.Updated
        assertEquals(GuessByNumbersStatus.WON, result.session.status)
        assertEquals(5, result.session.guesses.last().exact)
    }

    @Test
    fun `submitGuessByNumbersGuess marks the session LOST after the tenth wrong guess`() {
        var session = freshSession(target = "apple")
        repeat(9) {
            session = (submitGuessByNumbersGuess(session, dictionary, "grape") as GuessByNumbersSubmitResult.Updated).session
        }
        assertEquals(GuessByNumbersStatus.PLAYING, session.status)
        val final = (submitGuessByNumbersGuess(session, dictionary, "mango") as GuessByNumbersSubmitResult.Updated).session
        assertEquals(GuessByNumbersStatus.LOST, final.status)
        assertEquals(10, final.guesses.size)
    }

    @Test
    fun `guessByNumbersHasMeaningfulProgress is false with no guesses and true after one`() {
        val session = freshSession()
        assertEquals(false, guessByNumbersHasMeaningfulProgress(session))
        val afterGuess = (submitGuessByNumbersGuess(session, dictionary, "grape") as GuessByNumbersSubmitResult.Updated).session
        assertEquals(true, guessByNumbersHasMeaningfulProgress(afterGuess))
    }

    @Test
    fun `submitGuessByNumbersGuess is a no-op when called on a finished session`() {
        val wonSession = freshSession().copy(
            status = GuessByNumbersStatus.WON,
            guesses = listOf(GuessByNumbersGuessRow("apple", matches = 5, exact = 5)),
        )
        val wonResult = submitGuessByNumbersGuess(wonSession, dictionary, "qqqqq") as GuessByNumbersSubmitResult.Updated
        assertEquals(wonSession, wonResult.session)
    }
}
