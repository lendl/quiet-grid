package com.quietgrid.app.games.sudoku

import com.quietgrid.engine.sudoku.SudokuCellValue
import com.quietgrid.engine.sudoku.SudokuGrid
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import kotlinx.serialization.Serializable

enum class SudokuInputMode { DIGIT, NOTES }

data class SudokuSession(
    val puzzle: SudokuPuzzleEntry,
    val board: SudokuGrid,
    val notes: List<List<Set<Int>>>,
    val inputMode: SudokuInputMode,
    val accuracyDrops: Int,
    val finishedCells: List<List<Boolean>>,
    val penalizedUnitKeys: List<String>,
)

@Serializable
data class SudokuPersistedSession(
    val puzzle: SudokuPuzzleEntry,
    val board: List<Int?>,
    val notes: List<List<Int>>,
    val inputMode: SudokuInputMode,
    val accuracyDrops: Int,
    val finishedCells: List<Boolean>,
    val penalizedUnitKeys: List<String>,
)
