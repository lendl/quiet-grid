package com.quietgrid.engine.hashi

data class HashiHint(val edge: Int, val count: Int, val technique: HashiTechnique?, val isMistake: Boolean)

fun findHashiHint(puzzle: HashiPuzzleEntry, placed: IntArray): HashiHint? {
    val board = puzzle.toBoard()
    val solution = board.countsFromBridges(puzzle.solution)
    val mistake = board.edges.indices.firstOrNull { placed[it] > solution[it] }
    if (mistake != null) return HashiHint(mistake, solution[mistake], null, isMistake = true)
    if (board.edges.indices.all { placed[it] == solution[it] }) return null

    val state = HashiSolverState.fromPlaced(board, placed)
    while (!state.isSolved() && !hasHashiContradiction(state)) {
        val deduction = nextHashiDeduction(state) ?: break
        state.apply(deduction)
        val revealed = board.edges.indices.firstOrNull { state.lo(it) > placed[it] }
        if (revealed != null) return HashiHint(revealed, state.lo(revealed), deduction.technique, isMistake = false)
    }
    val fallback = board.edges.indices.first { placed[it] < solution[it] }
    return HashiHint(fallback, solution[fallback], null, isMistake = false)
}
