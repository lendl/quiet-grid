package com.quietgrid.engine.nonogram

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
