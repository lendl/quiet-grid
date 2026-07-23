package com.quietgrid.app.games.takuzu

import kotlinx.serialization.Serializable

/** 0, 1, or null (empty). */
typealias TakuzuCellValue = Int?
typealias TakuzuGrid = List<List<TakuzuCellValue>>

@Serializable
data class TakuzuPuzzleEntry(
    val id: String,
    val size: Int,
    val difficulty: String,
    val solution: String,
    val mask: String,
)

data class TakuzuSession(
    val puzzle: TakuzuPuzzleEntry,
    val board: TakuzuGrid,
    val solution: TakuzuGrid,
    val isGiven: List<List<Boolean>>,
    val finishedCells: List<List<Boolean>>,
    val accuracyDrops: Int,
    val penalizedLineKeys: List<String>,
)

/** Persisted shape: board/finishedCells as flattened lists, solution/isGiven re-derived from puzzle on restore. */
@Serializable
data class TakuzuPersistedSession(
    val puzzle: TakuzuPuzzleEntry,
    val board: List<Int?>,
    val finishedCells: List<Boolean>,
    val accuracyDrops: Int,
    val penalizedLineKeys: List<String>,
)

enum class CompletedLineState { INCOMPLETE, CORRECT, INCORRECT }
