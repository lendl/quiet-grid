package com.quietgrid.app.games.sudoku

import kotlinx.serialization.Serializable

typealias SudokuCellValue = Int?
typealias SudokuGrid = List<List<SudokuCellValue>>

enum class SudokuInputMode { DIGIT, NOTES }

@Serializable
data class SudokuPuzzleEntry(
    val id: String,
    val difficulty: String,
    val givens: List<List<Int?>>,
    val solution: List<List<Int>>,
)

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
