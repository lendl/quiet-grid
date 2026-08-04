package com.quietgrid.app.games.wordguess

import com.quietgrid.engine.wordguess.evaluateGuess
import com.quietgrid.engine.wordguess.isValidGuess

sealed interface WordGuessSubmitResult {
    data class Updated(val session: WordGuessSession) : WordGuessSubmitResult
    data object InvalidWord : WordGuessSubmitResult
}

fun submitWordGuess(session: WordGuessSession, dictionary: Set<String>, rawGuess: String): WordGuessSubmitResult {
    // No-op when game is already finished (WON or LOST); callers should stop submitting guesses once status leaves PLAYING.
    if (session.status != WordGuessStatus.PLAYING) return WordGuessSubmitResult.Updated(session)
    val guess = rawGuess.lowercase()
    if (guess.length != session.wordLength) return WordGuessSubmitResult.InvalidWord
    if (!isValidGuess(guess, dictionary)) return WordGuessSubmitResult.InvalidWord

    val feedback = evaluateGuess(session.targetWord, guess)
    val updatedGuesses = session.guesses + WordGuessGuessRow(guess, feedback)
    val won = guess == session.targetWord
    val lost = !won && updatedGuesses.size >= WORD_GUESS_MAX_GUESSES
    val status = when {
        won -> WordGuessStatus.WON
        lost -> WordGuessStatus.LOST
        else -> WordGuessStatus.PLAYING
    }
    return WordGuessSubmitResult.Updated(session.copy(guesses = updatedGuesses, status = status))
}

fun wordGuessHasMeaningfulProgress(session: WordGuessSession): Boolean = session.guesses.isNotEmpty()
