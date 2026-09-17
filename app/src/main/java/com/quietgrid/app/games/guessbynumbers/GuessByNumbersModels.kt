package com.quietgrid.app.games.guessbynumbers

import kotlinx.serialization.Serializable

const val GUESS_BY_NUMBERS_MAX_GUESSES = 10

@Serializable
enum class GuessByNumbersStatus { PLAYING, WON, LOST }

@Serializable
data class GuessByNumbersGuessRow(val guess: String, val matches: Int, val exact: Int)

data class GuessByNumbersSession(
    val puzzleId: String,
    val locale: String,
    val difficulty: String,
    val targetWord: String,
    val wordLength: Int,
    val guesses: List<GuessByNumbersGuessRow>,
    val status: GuessByNumbersStatus,
)

@Serializable
data class GuessByNumbersPersistedSession(
    val puzzleId: String,
    val locale: String,
    val difficulty: String,
    val targetWord: String,
    val wordLength: Int,
    val guesses: List<GuessByNumbersGuessRow>,
    val status: GuessByNumbersStatus,
)
