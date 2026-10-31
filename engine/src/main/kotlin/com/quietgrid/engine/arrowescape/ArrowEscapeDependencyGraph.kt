package com.quietgrid.engine.arrowescape

data class DependencyGraph(val dependsOn: List<List<Int>>, val blocks: List<List<Int>>)

fun buildCellOwnerMap(pieces: List<ArrowEscapePiece>): Map<Pair<Int, Int>, Int> {
    val owner = mutableMapOf<Pair<Int, Int>, Int>()
    pieces.forEachIndexed { index, piece ->
        piece.cells.forEach { owner[it.row to it.col] = index }
    }
    return owner
}

fun buildCellOwnerGrid(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int): IntArray {
    val owner = IntArray(rows * cols) { -1 }
    pieces.forEachIndexed { index, piece ->
        piece.cells.forEach { owner[it.row * cols + it.col] = index }
    }
    return owner
}

fun corridorOwners(piece: ArrowEscapePiece, owner: IntArray, rows: Int, cols: Int, limit: Int = Int.MAX_VALUE): List<Int> {
    val head = piece.cells.last()
    val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(piece.headDirection)
    val owners = mutableListOf<Int>()
    var r = head.row + dr
    var c = head.col + dc
    var steps = 0
    while (r in 0 until rows && c in 0 until cols && steps < limit) {
        val cellOwner = owner[r * cols + c]
        if (cellOwner >= 0 && cellOwner !in owners) owners.add(cellOwner)
        r += dr
        c += dc
        steps++
    }
    return owners
}

fun buildDependencyGraph(pieces: List<ArrowEscapePiece>, rows: Int, cols: Int): DependencyGraph {
    val owner = buildCellOwnerGrid(pieces, rows, cols)
    val dependsOn = pieces.map { corridorOwners(it, owner, rows, cols) }
    val blocks = List(pieces.size) { mutableListOf<Int>() }
    dependsOn.forEachIndexed { index, deps -> deps.forEach { depIndex -> blocks[depIndex].add(index) } }
    return DependencyGraph(dependsOn, blocks.map { it.toList() })
}
