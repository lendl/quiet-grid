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
    val wrongIndices: Set<Int>,
    val transform: Int = 0,
)

@Serializable
data class ArrowEscapePersistedSession(
    val puzzle: ArrowEscapePuzzleEntry? = null,
    val puzzleId: String? = null,
    val difficulty: String? = null,
    val transform: Int = 0,
    val removedIndices: List<Int>,
    val lives: Int,
    val selectedIndex: Int?,
    val status: String,
    val hintsUsed: Int = 0,
    val wrongIndices: List<Int> = emptyList(),
)

fun createArrowEscapeSession(puzzle: ArrowEscapePuzzleEntry, transform: Int = 0): ArrowEscapeSession = ArrowEscapeSession(
    puzzle = puzzle,
    removedIndices = emptySet(),
    lives = ARROW_ESCAPE_STARTING_LIVES,
    selectedIndex = null,
    status = ArrowEscapeStatus.PLAYING,
    hintsUsed = 0,
    wrongIndices = emptySet(),
    transform = transform,
)
