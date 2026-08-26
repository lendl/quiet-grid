package com.quietgrid.engine.arrowescape

data class DependencyGraph(val dependsOn: List<List<Int>>, val blocks: List<List<Int>>)

fun buildCellOwnerMap(pieces: List<ArrowEscapePiece>): Map<Pair<Int, Int>, Int> {
    val owner = mutableMapOf<Pair<Int, Int>, Int>()
    pieces.forEachIndexed { index, piece ->
        piece.cells.forEach { owner[it.row to it.col] = index }
    }
    return owner
}

fun buildDependencyGraph(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int): DependencyGraph {
    val owner = buildCellOwnerMap(pieces)
    val dependsOn = pieces.mapIndexed { index, piece ->
        val head = piece.cells.last()
        val corridor = computeCorridor(head.row, head.col, piece.headDirection, rows, cols)
        corridor.mapNotNull { owner[it.row to it.col] }.filter { it != index }.distinct()
    }
    val blocks = List(pieces.size) { mutableListOf<Int>() }
    dependsOn.forEachIndexed { index, deps -> deps.forEach { depIndex -> blocks[depIndex].add(index) } }
    return DependencyGraph(dependsOn, blocks.map { it.toList() })
}
