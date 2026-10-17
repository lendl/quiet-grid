package com.quietgrid.engine.minesweeper

internal fun nextMinesweeperDeductions(state: MinesweeperSolverState): List<MinesweeperDeduction> =
    clueCountDeductions(state)
        .ifEmpty { cluePairDeductions(state) }
        .ifEmpty { listOfNotNull(mineCountDeduction(state)) }

private fun clueCountDeductions(state: MinesweeperSolverState): List<MinesweeperDeduction> {
    val result = mutableListOf<MinesweeperDeduction>()
    for (clue in 0 until state.size) {
        if (!state.isOpen(clue)) continue
        val unknown = state.unknownNeighbors(clue)
        if (unknown.isEmpty()) continue
        val remaining = state.remainingMines(clue)
        if (remaining == 0) {
            result.add(MinesweeperDeduction(MinesweeperTechnique.CLUE_COUNT, listOf(clue), safe = unknown, mines = emptyList()))
        } else if (remaining == unknown.size) {
            result.add(MinesweeperDeduction(MinesweeperTechnique.CLUE_COUNT, listOf(clue), safe = emptyList(), mines = unknown))
        }
    }
    return result
}

private fun cluePairDeductions(state: MinesweeperSolverState): List<MinesweeperDeduction> {
    val unknownAround = arrayOfNulls<List<Int>>(state.size)
    for (cell in 0 until state.size) {
        if (!state.isOpen(cell)) continue
        val unknown = state.unknownNeighbors(cell)
        if (unknown.isNotEmpty()) unknownAround[cell] = unknown
    }
    val result = mutableListOf<MinesweeperDeduction>()
    for (a in 0 until state.size) {
        val unknownA = unknownAround[a] ?: continue
        for (b in state.nearby[a]) {
            if (b <= a) continue
            val unknownB = unknownAround[b] ?: continue
            cluePairDeduction(state, a, b, unknownA, unknownB)?.let(result::add)
        }
    }
    return result
}

private fun cluePairDeduction(
    state: MinesweeperSolverState,
    a: Int,
    b: Int,
    unknownA: List<Int>,
    unknownB: List<Int>,
): MinesweeperDeduction? {
    val shared = unknownA.filter { it in unknownB }
    if (shared.isEmpty()) return null
    val onlyA = unknownA.filter { it !in shared }
    val onlyB = unknownB.filter { it !in shared }
    val remainingA = state.remainingMines(a)
    val remainingB = state.remainingMines(b)
    val minShared = maxOf(0, remainingA - onlyA.size, remainingB - onlyB.size)
    val maxShared = minOf(shared.size, remainingA, remainingB)

    val safe = mutableListOf<Int>()
    val mines = mutableListOf<Int>()
    classifyGroup(onlyA, remainingA - maxShared, remainingA - minShared, safe, mines)
    classifyGroup(onlyB, remainingB - maxShared, remainingB - minShared, safe, mines)
    classifyGroup(shared, minShared, maxShared, safe, mines)
    if (safe.isEmpty() && mines.isEmpty()) return null
    return MinesweeperDeduction(MinesweeperTechnique.CLUE_PAIR, listOf(a, b), safe.sorted(), mines.sorted())
}

private fun classifyGroup(cells: List<Int>, minMines: Int, maxMines: Int, safe: MutableList<Int>, mines: MutableList<Int>) {
    if (cells.isEmpty()) return
    if (maxMines == 0) safe.addAll(cells) else if (minMines == cells.size) mines.addAll(cells)
}

private fun mineCountDeduction(state: MinesweeperSolverState): MinesweeperDeduction? {
    if (state.unknownCount == 0) return null
    val remaining = state.totalMines - state.mineCount
    val unknown = (0 until state.size).filter { state.isUnknown(it) }
    return when (remaining) {
        0 -> MinesweeperDeduction(MinesweeperTechnique.MINE_COUNT, emptyList(), safe = unknown, mines = emptyList())
        unknown.size -> MinesweeperDeduction(MinesweeperTechnique.MINE_COUNT, emptyList(), safe = emptyList(), mines = unknown)
        else -> null
    }
}
