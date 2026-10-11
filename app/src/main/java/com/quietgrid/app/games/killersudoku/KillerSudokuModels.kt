package com.quietgrid.app.games.killersudoku

import com.quietgrid.app.games.sudoku.SudokuInputMode
import com.quietgrid.app.games.sudoku.SudokuSession
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import kotlinx.serialization.Serializable

data class KillerSudokuSession(
    val puzzle: KillerSudokuPuzzleEntry,
    val grid: SudokuSession,
) {
    val layout: KillerCageLayout by lazy { KillerCageLayout(puzzle.cages) }
}

@Serializable
data class KillerSudokuPersistedSession(
    val puzzle: KillerSudokuPuzzleEntry,
    val board: List<Int?>,
    val notes: List<List<Int>>,
    val inputMode: SudokuInputMode,
    val accuracyDrops: Int,
    val finishedCells: List<Boolean>,
    val penalizedUnitKeys: List<String>,
    val autoCandidateMode: Boolean = false,
)
