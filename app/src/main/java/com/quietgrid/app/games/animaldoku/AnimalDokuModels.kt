// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuModels.kt
package com.quietgrid.app.games.animaldoku

import androidx.annotation.StringRes
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuUnit
import kotlinx.serialization.Serializable

const val ANIMALDOKU_STARTING_LIVES = 3

enum class AnimalDokuCellState { EMPTY, MARKED, LOCKED_CORRECT, LOCKED_WRONG }

enum class AnimalDokuStatus { PLAYING, WON, LOST }

data class AnimalDokuSession(
    val puzzle: AnimalDokuPuzzleEntry,
    val cells: List<List<AnimalDokuCellState>>,
    val lives: Int,
    val status: AnimalDokuStatus,
)

@Serializable
data class AnimalDokuPersistedSession(
    val puzzle: AnimalDokuPuzzleEntry,
    val cells: List<Int>,
    val lives: Int,
    val status: String,
)

fun createAnimalDokuSession(puzzle: AnimalDokuPuzzleEntry): AnimalDokuSession = AnimalDokuSession(
    puzzle = puzzle,
    cells = List(puzzle.size) { List(puzzle.size) { AnimalDokuCellState.EMPTY } },
    lives = ANIMALDOKU_STARTING_LIVES,
    status = AnimalDokuStatus.PLAYING,
)

data class AnimalDokuHintCopy(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val unitArgs: List<List<AnimalDokuUnit>> = emptyList(),
)

enum class AnimalDokuHintTone { FOCUS, CONTEXT, DIMMED }

data class AnimalDokuHintHighlight(
    val focusCells: Set<Pair<Int, Int>>,
    val unitCells: Set<Pair<Int, Int>> = emptySet(),
    val crossCells: Set<Pair<Int, Int>> = emptySet(),
    val animalCell: Pair<Int, Int>? = null,
) {
    fun toneOf(cell: Pair<Int, Int>): AnimalDokuHintTone = when (cell) {
        in focusCells -> AnimalDokuHintTone.FOCUS
        in unitCells -> AnimalDokuHintTone.CONTEXT
        else -> AnimalDokuHintTone.DIMMED
    }
}
