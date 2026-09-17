package com.quietgrid.app.games.guessbynumbers

import com.quietgrid.engine.wordguess.LetterState
import com.quietgrid.engine.wordguess.evaluateGuess
import com.quietgrid.engine.wordguess.isValidGuess

sealed interface GuessByNumbersSubmitResult {
    data class Updated(val session: GuessByNumbersSession) : GuessByNumbersSubmitResult
    data object InvalidWord : GuessByNumbersSubmitResult
}

fun computeMatchesAndExact(target: String, guess: String): Pair<Int, Int> {
    val states = evaluateGuess(target, guess)
    val exact = states.count { it == LetterState.CORRECT }
    val matches = exact + states.count { it == LetterState.PRESENT }
    return matches to exact
}

fun submitGuessByNumbersGuess(session: GuessByNumbersSession, dictionary: Set<String>, rawGuess: String): GuessByNumbersSubmitResult {
    if (session.status != GuessByNumbersStatus.PLAYING) return GuessByNumbersSubmitResult.Updated(session)
    val guess = rawGuess.lowercase()
    if (guess.length != session.wordLength) return GuessByNumbersSubmitResult.InvalidWord
    if (!isValidGuess(guess, dictionary)) return GuessByNumbersSubmitResult.InvalidWord

    val (matches, exact) = computeMatchesAndExact(session.targetWord, guess)
    val updatedGuesses = session.guesses + GuessByNumbersGuessRow(guess, matches, exact)
    val won = exact == session.wordLength
    val lost = !won && updatedGuesses.size >= GUESS_BY_NUMBERS_MAX_GUESSES
    val status = when {
        won -> GuessByNumbersStatus.WON
        lost -> GuessByNumbersStatus.LOST
        else -> GuessByNumbersStatus.PLAYING
    }
    return GuessByNumbersSubmitResult.Updated(session.copy(guesses = updatedGuesses, status = status))
}

fun guessByNumbersHasMeaningfulProgress(session: GuessByNumbersSession): Boolean = session.guesses.isNotEmpty()
