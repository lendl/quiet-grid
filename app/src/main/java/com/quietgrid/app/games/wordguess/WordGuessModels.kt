package com.quietgrid.app.games.wordguess

import com.quietgrid.engine.wordguess.LetterState
import kotlinx.serialization.Serializable

const val WORD_GUESS_MAX_GUESSES = 6

@Serializable
enum class WordGuessStatus { PLAYING, WON, LOST }

@Serializable
data class WordGuessGuessRow(val guess: String, val feedback: List<LetterState>)

data class WordGuessSession(
    val puzzleId: String,
    val locale: String,
    val difficulty: String,
    val targetWord: String,
    val wordLength: Int,
    val guesses: List<WordGuessGuessRow>,
    val status: WordGuessStatus,
)

@Serializable
data class WordGuessPersistedSession(
    val puzzleId: String,
    val locale: String,
    val difficulty: String,
    val targetWord: String,
    val wordLength: Int,
    val guesses: List<WordGuessGuessRow>,
    val status: WordGuessStatus,
)
