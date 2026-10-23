package com.quietgrid.engine.links

data class LinksStroke(
    val color: Int,
    val base: List<List<Int>>,
    val path: List<Int>,
)

fun emptyLinksPaths(puzzle: LinksPuzzleEntry): List<List<Int>> = List(puzzle.pairCount) { emptyList() }

fun linksOwners(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): IntArray {
    val owners = IntArray(puzzle.cellCount) { -1 }
    puzzle.endpoints.forEachIndexed { color, ends -> ends.forEach { owners[it] = color } }
    paths.forEachIndexed { color, path -> path.forEach { owners[it] = color } }
    return owners
}

fun isLinksPairConnected(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, color: Int): Boolean {
    val path = paths.getOrNull(color) ?: return false
    if (path.size < 2) return false
    val ends = puzzle.endpoints[color]
    return (path.first() == ends[0] && path.last() == ends[1]) || (path.first() == ends[1] && path.last() == ends[0])
}

fun linksConnectedCount(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): Int =
    (0 until puzzle.pairCount).count { isLinksPairConnected(puzzle, paths, it) }

fun linksFilledCount(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): Int =
    linksOwners(puzzle, paths).count { it >= 0 }

fun isLinksSolved(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): Boolean =
    linksConnectedCount(puzzle, paths) == puzzle.pairCount && linksFilledCount(puzzle, paths) == puzzle.cellCount

fun startLinksStroke(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, cell: Int): LinksStroke? {
    if (cell !in 0 until puzzle.cellCount) return null
    puzzle.endpointColorAt(cell)?.let { color -> return LinksStroke(color, paths, listOf(cell)) }
    paths.forEachIndexed { color, path ->
        val index = path.indexOf(cell)
        if (index >= 0) return LinksStroke(color, paths, path.subList(0, index + 1).toList())
    }
    return null
}

fun extendLinksStroke(puzzle: LinksPuzzleEntry, stroke: LinksStroke, toCell: Int): LinksStroke {
    if (toCell !in 0 until puzzle.cellCount) return stroke
    val size = puzzle.size
    var current = stroke
    var row = current.path.last() / size
    var col = current.path.last() % size
    val targetRow = toCell / size
    val targetCol = toCell % size
    while (row != targetRow || col != targetCol) {
        val dr = targetRow - row
        val dc = targetCol - col
        if (kotlin.math.abs(dc) >= kotlin.math.abs(dr)) col += Integer.signum(dc) else row += Integer.signum(dr)
        val next = stepLinksStroke(puzzle, current, row * size + col)
        if (next === current && current.path.last() != row * size + col) break
        current = next
    }
    return current
}

private fun stepLinksStroke(puzzle: LinksPuzzleEntry, stroke: LinksStroke, cell: Int): LinksStroke {
    val path = stroke.path
    val head = path.last()
    if (!linksAreAdjacent(head, cell, puzzle.size)) return stroke
    val existing = path.indexOf(cell)
    if (existing >= 0) return stroke.copy(path = path.subList(0, existing + 1).toList())
    val ends = puzzle.endpoints[stroke.color]
    val goal = if (path.first() == ends[0]) ends[1] else ends[0]
    if (path.size > 1 && head == goal) return stroke
    val endpointColor = puzzle.endpointColorAt(cell)
    if (endpointColor != null && (endpointColor != stroke.color || cell != goal)) return stroke
    return stroke.copy(path = path + cell)
}

fun linksStrokePaths(stroke: LinksStroke): List<List<Int>> {
    val drawn = stroke.path.toHashSet()
    return stroke.base.mapIndexed { color, path ->
        if (color == stroke.color) {
            stroke.path
        } else {
            val cut = path.indexOfFirst { it in drawn }
            if (cut < 0) path else path.subList(0, cut).toList()
        }
    }
}

fun finishLinksStroke(stroke: LinksStroke): List<List<Int>> =
    linksStrokePaths(stroke).map { if (it.size < 2) emptyList() else it }

fun linksAccuracyPercent(pairCount: Int, moves: Int, hintsUsed: Int): Int {
    val spent = moves + hintsUsed * 2
    if (pairCount <= 0) return 100
    return (pairCount * 100 / maxOf(pairCount, spent)).coerceIn(0, 100)
}

private fun matchesSolution(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, color: Int): Boolean {
    val path = paths.getOrNull(color) ?: return false
    val solution = puzzle.solution[color]
    return path == solution || path == solution.asReversed()
}

fun linksHintColor(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): Int? =
    (0 until puzzle.pairCount)
        .filter { !matchesSolution(puzzle, paths, it) }
        .minByOrNull { puzzle.solution[it].size }

fun applyLinksSolutionPath(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, color: Int): List<List<Int>> =
    finishLinksStroke(LinksStroke(color, paths, puzzle.solution[color]))
