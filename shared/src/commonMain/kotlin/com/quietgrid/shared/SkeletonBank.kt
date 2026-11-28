package com.quietgrid.shared

import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import kotlinx.serialization.json.Json

const val SKELETON_BANK_FILE = "sudoku_puzzles.json"

private val bankJson = Json { ignoreUnknownKeys = true }

fun firstSudokuEntry(bank: String): SudokuPuzzleEntry =
    bankJson.decodeFromString<List<SudokuPuzzleEntry>>(bank).first()
