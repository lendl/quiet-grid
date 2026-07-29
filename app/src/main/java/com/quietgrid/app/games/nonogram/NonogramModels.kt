package com.quietgrid.app.games.nonogram

import com.quietgrid.engine.nonogram.NonogramCellValue
import com.quietgrid.engine.nonogram.NonogramGrid
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import kotlinx.serialization.Serializable

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
