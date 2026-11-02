package com.quietgrid.engine.journey

enum class GridTransform(val id: Int) {
    IDENTITY(0),
    MIRROR_H(1),
    ROTATE_90(2),
    ROTATE_180(3),
    ROTATE_270(4),
    MIRROR_V(5),
    TRANSPOSE(6),
    ANTI_TRANSPOSE(7),
}

fun gridTransformById(id: Int): GridTransform = GridTransform.entries.firstOrNull { it.id == id } ?: GridTransform.IDENTITY

fun gridTransformsFor(rows: Int, cols: Int): List<GridTransform> =
    if (rows == cols) {
        GridTransform.entries.toList()
    } else {
        listOf(GridTransform.IDENTITY, GridTransform.MIRROR_H, GridTransform.ROTATE_180, GridTransform.MIRROR_V)
    }

private val GridTransform.swapsAxes: Boolean
    get() = this == GridTransform.ROTATE_90 || this == GridTransform.ROTATE_270 || this == GridTransform.TRANSPOSE || this == GridTransform.ANTI_TRANSPOSE

fun GridTransform.outputSize(rows: Int, cols: Int): Pair<Int, Int> = if (swapsAxes) cols to rows else rows to cols

fun GridTransform.mapCell(row: Int, col: Int, rows: Int, cols: Int): Pair<Int, Int> = when (this) {
    GridTransform.IDENTITY -> row to col
    GridTransform.MIRROR_H -> row to cols - 1 - col
    GridTransform.ROTATE_90 -> col to rows - 1 - row
    GridTransform.ROTATE_180 -> rows - 1 - row to cols - 1 - col
    GridTransform.ROTATE_270 -> cols - 1 - col to row
    GridTransform.MIRROR_V -> rows - 1 - row to col
    GridTransform.TRANSPOSE -> col to row
    GridTransform.ANTI_TRANSPOSE -> cols - 1 - col to rows - 1 - row
}

fun GridTransform.mapVector(dRow: Int, dCol: Int): Pair<Int, Int> {
    val (originRow, originCol) = mapCell(1, 1, 3, 3)
    val (row, col) = mapCell(1 + dRow, 1 + dCol, 3, 3)
    return (row - originRow) to (col - originCol)
}
