package com.quietgrid.engine.takuzu

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
