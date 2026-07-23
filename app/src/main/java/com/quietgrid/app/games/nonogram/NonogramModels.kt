package com.quietgrid.app.games.nonogram

import kotlinx.serialization.Serializable

/** 0 = marked empty, 1 = filled, null = blank. */
typealias NonogramCellValue = Int?
typealias NonogramGrid = List<List<NonogramCellValue>>

@Serializable
data class NonogramPuzzleEntry(
    val id: String,
    val difficulty: String,
    val rows: Int,
    val cols: Int,
    val solution: List<List<Boolean>>,
)

data class NonogramPuzzle(
    val id: String,
    val difficulty: String,
    val rows: Int,
    val cols: Int,
    val rowClues: List<List<Int>>,
    val colClues: List<List<Int>>,
)

data class NonogramSession(
    val puzzle: NonogramPuzzle,
    val board: NonogramGrid,
    val solution: List<List<Boolean>>,
)

@Serializable
data class NonogramPersistedSession(
    val entry: NonogramPuzzleEntry,
    val board: List<Int?>,
)

enum class NonogramInputMode { FILL, CROSS }
