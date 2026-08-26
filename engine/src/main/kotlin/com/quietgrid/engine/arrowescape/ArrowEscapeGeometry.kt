package com.quietgrid.engine.arrowescape

val ARROW_DIRECTION_DELTA: Map<ArrowDirection, Pair<Int, Int>> = mapOf(
    ArrowDirection.UP to (-1 to 0),
    ArrowDirection.DOWN to (1 to 0),
    ArrowDirection.LEFT to (0 to -1),
    ArrowDirection.RIGHT to (0 to 1),
)

fun computeCorridor(headRow: Int, headCol: Int, direction: ArrowDirection, rows: Int, cols: Int): List<CellCoord> {
    val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(direction)
    val corridor = mutableListOf<CellCoord>()
    var r = headRow + dr
    var c = headCol + dc
    while (r in 0 until rows && c in 0 until cols) {
        corridor.add(CellCoord(r, c))
        r += dr
        c += dc
    }
    return corridor
}
