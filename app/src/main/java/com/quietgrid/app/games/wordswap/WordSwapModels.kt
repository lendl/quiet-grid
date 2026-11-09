package com.quietgrid.app.games.wordswap

import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import kotlinx.serialization.Serializable

enum class WordSwapStatus { PLAYING, WON }

data class WordSwapSession(
    val puzzle: WordSwapPuzzleEntry,
    val board: String,
    val swaps: Int,
    val hintsUsed: Int,
    val status: WordSwapStatus,
)

@Serializable
data class WordSwapPersistedSession(
    val puzzle: WordSwapPuzzleEntry,
    val board: String,
    val swaps: Int,
    val hintsUsed: Int,
    val status: String,
)

fun createWordSwapSession(puzzle: WordSwapPuzzleEntry): WordSwapSession = WordSwapSession(
    puzzle = puzzle,
    board = puzzle.start,
    swaps = 0,
    hintsUsed = 0,
    status = WordSwapStatus.PLAYING,
)
