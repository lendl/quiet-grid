package com.quietgrid.app.games.takuzu

import com.quietgrid.engine.takuzu.TakuzuCellValue
import com.quietgrid.engine.takuzu.TakuzuGrid
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import kotlinx.serialization.Serializable

data class TakuzuSession(
    val puzzle: TakuzuPuzzleEntry,
    val board: TakuzuGrid,
    val solution: TakuzuGrid,
    val isGiven: List<List<Boolean>>,
    val finishedCells: List<List<Boolean>>,
    val accuracyDrops: Int,
    val penalizedLineKeys: List<String>,
)

@Serializable
data class TakuzuPersistedSession(
    val puzzle: TakuzuPuzzleEntry,
    val board: List<Int?>,
    val finishedCells: List<Boolean>,
    val accuracyDrops: Int,
    val penalizedLineKeys: List<String>,
)

enum class CompletedLineState { INCOMPLETE, CORRECT, INCORRECT }
