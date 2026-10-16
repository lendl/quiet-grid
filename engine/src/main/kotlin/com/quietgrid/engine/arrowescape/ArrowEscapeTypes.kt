package com.quietgrid.engine.arrowescape

import kotlinx.serialization.Serializable

enum class ArrowDirection { UP, DOWN, LEFT, RIGHT }

val ArrowDirection.key: String get() = name.lowercase()

fun arrowDirectionFromKey(key: String): ArrowDirection = ArrowDirection.entries.first { it.key == key }

data class CellCoord(val row: Int, val col: Int)

data class ArrowEscapePiece(val cells: List<CellCoord>, val headDirection: ArrowDirection)

@Serializable
data class ArrowEscapePieceData(val cells: List<List<Int>>, val headDirection: String)

@Serializable
data class ArrowEscapePuzzleEntry(
    val id: String,
    val difficulty: String,
    val rows: Int,
    val cols: Int,
    val pieces: List<ArrowEscapePieceData>,
    val shape: String? = null,
    val mask: List<String>? = null,
)

const val ARROW_ESCAPE_MASK_ACTIVE = '#'
const val ARROW_ESCAPE_MASK_VOID = '.'

fun ArrowEscapePuzzleEntry.isActiveCell(row: Int, col: Int): Boolean {
    if (row !in 0 until rows || col !in 0 until cols) return false
    val maskRows = mask ?: return true
    return maskRows.getOrNull(row)?.getOrNull(col) == ARROW_ESCAPE_MASK_ACTIVE
}

fun ArrowEscapePieceData.toPiece(): ArrowEscapePiece = ArrowEscapePiece(
    cells = cells.map { CellCoord(it[0], it[1]) },
    headDirection = arrowDirectionFromKey(headDirection),
)
