package com.quietgrid.app.games.wordguess

import com.quietgrid.engine.wordguess.LetterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGuessLogicTest {
    private val dictionary = setOf("apple", "grape", "mango", "zzzzz")

    private fun freshSession(target: String = "apple") = WordGuessSession(
        puzzleId = "en-easy-apple",
        locale = "en",
        difficulty = "easy",
        targetWord = target,
        wordLength = target.length,
        guesses = emptyList(),
        status = WordGuessStatus.PLAYING,
    )

    @Test
    fun `submitWordGuess rejects a guess not in the dictionary, leaving the session untouched`() {
        val session = freshSession()
        val result = submitWordGuess(session, dictionary, "qqqqq")
        assertTrue(result is WordGuessSubmitResult.InvalidWord)
    }

    @Test
    fun `submitWordGuess appends a valid guess and stays PLAYING when wrong`() {
        val session = freshSession()
        val result = submitWordGuess(session, dictionary, "grape") as WordGuessSubmitResult.Updated
        assertEquals(1, result.session.guesses.size)
        assertEquals(WordGuessStatus.PLAYING, result.session.status)
    }

    @Test
    fun `submitWordGuess marks the session WON on an exact match`() {
        val session = freshSession()
        val result = submitWordGuess(session, dictionary, "apple") as WordGuessSubmitResult.Updated
        assertEquals(WordGuessStatus.WON, result.session.status)
    }

    @Test
    fun `submitWordGuess marks the session LOST after the sixth wrong guess`() {
        var session = freshSession(target = "apple")
        repeat(5) {
            session = (submitWordGuess(session, dictionary, "grape") as WordGuessSubmitResult.Updated).session
        }
        assertEquals(WordGuessStatus.PLAYING, session.status)
        val final = (submitWordGuess(session, dictionary, "mango") as WordGuessSubmitResult.Updated).session
        assertEquals(WordGuessStatus.LOST, final.status)
        assertEquals(6, final.guesses.size)
    }

    @Test
    fun `wordGuessHasMeaningfulProgress is false with no guesses and true after one`() {
        val session = freshSession()
        assertEquals(false, wordGuessHasMeaningfulProgress(session))
        val afterGuess = (submitWordGuess(session, dictionary, "grape") as WordGuessSubmitResult.Updated).session
        assertEquals(true, wordGuessHasMeaningfulProgress(afterGuess))
    }

    @Test
    fun `submitWordGuess is a no-op when called on a finished session, returning unchanged session`() {
        // Calling on WON: invalid guess that would normally be rejected is silently ignored
        val wonSession = freshSession().copy(status = WordGuessStatus.WON, guesses = listOf(
            WordGuessGuessRow("apple", listOf(LetterState.CORRECT, LetterState.CORRECT, LetterState.CORRECT, LetterState.CORRECT, LetterState.CORRECT))
        ))
        val wonResult = submitWordGuess(wonSession, dictionary, "qqqqq") as WordGuessSubmitResult.Updated
        assertEquals(wonSession, wonResult.session)
        assertEquals(WordGuessStatus.WON, wonResult.session.status)

        // Calling on LOST: invalid guess is silently ignored
        val lostSession = freshSession().copy(status = WordGuessStatus.LOST, guesses = listOf(
            WordGuessGuessRow("grape", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT)),
            WordGuessGuessRow("mango", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT)),
            WordGuessGuessRow("zzzzz", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT)),
            WordGuessGuessRow("aaaaa", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT)),
            WordGuessGuessRow("eeeee", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT)),
            WordGuessGuessRow("iiiii", listOf(LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT))
        ))
        val lostResult = submitWordGuess(lostSession, dictionary, "qqqqq") as WordGuessSubmitResult.Updated
        assertEquals(lostSession, lostResult.session)
        assertEquals(WordGuessStatus.LOST, lostResult.session.status)
    }
}
