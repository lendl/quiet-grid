package com.quietgrid.engine.sudoku

import kotlinx.serialization.Serializable

const val SUDOKU_SIZE = 9
const val SUDOKU_BOX_SIZE = 3

typealias SudokuCellValue = Int?
typealias SudokuGrid = List<List<SudokuCellValue>>

@Serializable
data class SudokuPuzzleEntry(
    val id: String,
    val difficulty: String,
    val givens: List<List<Int?>>,
    val solution: List<List<Int>>,
)
