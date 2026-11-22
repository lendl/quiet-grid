package com.quietgrid.engine.links

data class LinksHint(val color: Int, val technique: LinksTechnique?, val isMistake: Boolean)

fun findLinksHint(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): LinksHint? {
    val colors = 0 until puzzle.pairCount
    val mistake = colors
        .filter { !isLinksPathOnTrack(puzzle, paths, it) }
        .maxByOrNull { paths[it].size }
    if (mistake != null) return LinksHint(mistake, null, isMistake = true)
    if (colors.all { isLinksPathComplete(puzzle, paths, it) }) return null

    val state = linksSolverStateFrom(puzzle, paths)
    val deduction = if (state.isFeasible()) nextLinksDeduction(state) else null
    if (deduction != null) return LinksHint(deduction.move.color, deduction.technique, isMistake = false)
    val fallback = colors.first { !isLinksPathComplete(puzzle, paths, it) }
    return LinksHint(fallback, null, isMistake = false)
}

private fun isLinksPathOnTrack(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, color: Int): Boolean {
    val path = paths.getOrNull(color).orEmpty()
    if (path.isEmpty()) return true
    val solution = puzzle.solution[color]
    if (path.size > solution.size) return false
    return path == solution.subList(0, path.size) || path == solution.asReversed().subList(0, path.size)
}

private fun isLinksPathComplete(puzzle: LinksPuzzleEntry, paths: List<List<Int>>, color: Int): Boolean =
    paths.getOrNull(color).orEmpty().size == puzzle.solution[color].size && isLinksPathOnTrack(puzzle, paths, color)

private fun linksSolverStateFrom(puzzle: LinksPuzzleEntry, paths: List<List<Int>>): LinksSolverState {
    val state = LinksSolverState.of(puzzle.size, puzzle.endpoints)
    paths.forEachIndexed { color, path ->
        if (path.size < 2) return@forEachIndexed
        val side = if (path.first() == puzzle.endpoints[color][0]) 0 else 1
        state.extendAlong(color, side, path.drop(1))
    }
    return state
}
