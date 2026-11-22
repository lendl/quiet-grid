package com.quietgrid.engine.wordsearch

val directionToDelta: Map<WordSearchDirection, Pair<Int, Int>> = mapOf(
    WordSearchDirection.RIGHT to (0 to 1),
    WordSearchDirection.LEFT to (0 to -1),
    WordSearchDirection.DOWN to (1 to 0),
    WordSearchDirection.UP to (-1 to 0),
    WordSearchDirection.DOWN_RIGHT to (1 to 1),
    WordSearchDirection.DOWN_LEFT to (1 to -1),
    WordSearchDirection.UP_RIGHT to (-1 to 1),
    WordSearchDirection.UP_LEFT to (-1 to -1),
)

fun toGridKey(cell: WSCellRef): Int = cell.row * 1000 + cell.col
