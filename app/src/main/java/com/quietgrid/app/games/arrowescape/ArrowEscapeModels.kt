package com.quietgrid.app.games.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.serialization.Serializable

const val ARROW_ESCAPE_STARTING_LIVES = 3

enum class ArrowEscapeStatus { PLAYING, WON, LOST }

data class ArrowEscapeSession(
    val puzzle: ArrowEscapePuzzleEntry,
    val removedIndices: Set<Int>,
    val lives: Int,
    val selectedIndex: Int?,
    val status: ArrowEscapeStatus,
    val hintsUsed: Int,
)

@Serializable
data class ArrowEscapePersistedSession(
    val puzzle: ArrowEscapePuzzleEntry,
    val removedIndices: List<Int>,
    val lives: Int,
    val selectedIndex: Int?,
    val status: String,
    val hintsUsed: Int = 0,
)

fun createArrowEscapeSession(puzzle: ArrowEscapePuzzleEntry): ArrowEscapeSession = ArrowEscapeSession(
    puzzle = puzzle,
    removedIndices = emptySet(),
    lives = ARROW_ESCAPE_STARTING_LIVES,
    selectedIndex = null,
    status = ArrowEscapeStatus.PLAYING,
    hintsUsed = 0,
)
