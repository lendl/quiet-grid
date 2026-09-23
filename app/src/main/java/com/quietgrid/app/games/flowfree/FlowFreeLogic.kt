package com.quietgrid.app.games.flowfree

fun flowFreeStartDrag(session: FlowFreeSession, row: Int, col: Int): FlowFreeSession {
    val color = session.puzzle.endpoints[row][col]
    if (color == -1) return session
    val startCell = row to col
    val existing = session.paths[color]
    val newPath = if (existing != null && startCell in existing) {
        existing.subList(0, existing.indexOf(startCell) + 1)
    } else {
        listOf(startCell)
    }
    return session.copy(paths = session.paths + (color to newPath), activeColor = color)
}

fun flowFreeExtendDrag(session: FlowFreeSession, row: Int, col: Int): FlowFreeSession {
    val color = session.activeColor ?: return session
    val path = session.paths[color] ?: return session
    val cell = row to col

    val existingIndex = path.indexOf(cell)
    if (existingIndex != -1) {
        return session.copy(paths = session.paths + (color to path.subList(0, existingIndex + 1)))
    }

    val head = path.last()
    if (kotlin.math.abs(head.first - row) + kotlin.math.abs(head.second - col) != 1) return session

    val occupiedByOtherColor = session.paths.any { (otherColor, cells) -> otherColor != color && cell in cells }
    if (occupiedByOtherColor) return session

    val cellEndpointColor = session.puzzle.endpoints[row][col]
    if (cellEndpointColor != -1 && cellEndpointColor != color) return session

    return session.copy(paths = session.paths + (color to (path + cell)))
}

fun flowFreeEndDrag(session: FlowFreeSession): FlowFreeSession = session.copy(activeColor = null)

fun flowFreeIsSolved(session: FlowFreeSession): Boolean {
    val size = session.puzzle.size
    val coveredCells = session.paths.values.flatten()
    if (coveredCells.size != size * size) return false
    if (coveredCells.toSet().size != coveredCells.size) return false

    return (0 until session.puzzle.pairCount).all { color ->
        val path = session.paths[color] ?: return false
        if (path.size < 2) return false
        val pathEnds = setOf(path.first(), path.last())
        val puzzleEndpoints = (0 until size)
            .flatMap { r -> (0 until size).mapNotNull { c -> if (session.puzzle.endpoints[r][c] == color) r to c else null } }
            .toSet()
        pathEnds == puzzleEndpoints
    }
}
