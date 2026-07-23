package com.quietgrid.app.games.minesweeper

/**
 * Ports the player-facing half of the RN app's Minesweeper next-move hint
 * (src/games/minesweeper/gameplay/analysis/nextMove.ts). That 997-line file also builds a
 * multi-step "logical move" trace and mine-flagging hints for the deferred full Puzzle
 * Analysis screen; this port only covers what getMinesweeperNextMoveHint() actually calls: a
 * fixed-point forced-mine propagation (direct local + subset-difference deduction, used only to
 * seed reasoning — never surfaced as a flag suggestion itself) feeding five safe-reveal
 * techniques in RN's exact priority order (only-one-possible-mine, guaranteed-safe-tile,
 * single-mine-logic, full-clue-resolution, all-mines-accounted-for), falling back to a generic
 * "guess" hint. This is the complete logic behind the single-hint feature — nothing skipped
 * within that scope.
 */

enum class MinesweeperHintPattern { ONLY_ONE_POSSIBLE_MINE, GUARANTEED_SAFE_TILE, SINGLE_MINE_LOGIC, FULL_CLUE_RESOLUTION, ALL_MINES_ACCOUNTED_FOR }

sealed interface MinesweeperNextMoveHint {
    val evidenceCells: List<Pair<Int, Int>>
    val targetCells: List<Pair<Int, Int>>
}

data class MinesweeperPatternHint(
    val pattern: MinesweeperHintPattern,
    val clueCell: Pair<Int, Int>?,
    val secondaryClueCell: Pair<Int, Int>?,
    val mineCount: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<Pair<Int, Int>>,
) : MinesweeperNextMoveHint

data object MinesweeperGuessHint : MinesweeperNextMoveHint {
    override val evidenceCells = emptyList<Pair<Int, Int>>()
    override val targetCells = emptyList<Pair<Int, Int>>()
}

private data class MSClue(val row: Int, val col: Int, val adjacentMines: Int)

private data class MSClueState(
    val unresolvedNeighbors: List<Pair<Int, Int>>,
    val forcedMineNeighbors: List<Pair<Int, Int>>,
    val remainingMines: Int,
)

private data class MSAnalyzedHint(
    val evidenceCells: List<Pair<Int, Int>>,
    val targetCells: List<Pair<Int, Int>>,
    val primaryClueCell: Pair<Int, Int>? = null,
    val secondaryClueCell: Pair<Int, Int>? = null,
    val mineCount: Int? = null,
)

private fun neighbors(board: MinesweeperBoard, row: Int, col: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    for (dr in -1..1) for (dc in -1..1) {
        if (dr == 0 && dc == 0) continue
        val r = row + dr; val c = col + dc
        if (r in 0 until board.rows && c in 0 until board.cols) result.add(r to c)
    }
    return result
}

private fun isUnknown(board: MinesweeperBoard, cell: Pair<Int, Int>): Boolean =
    board.cells[cell.first][cell.second].state != MinesweeperCellState.REVEALED

private fun compareCells(a: Pair<Int, Int>, b: Pair<Int, Int>): Int =
    if (a.first != b.first) a.first - b.first else a.second - b.second

private fun dedupeCells(cells: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
    cells.distinct().sortedWith(::compareCells)

private fun getClues(board: MinesweeperBoard): List<MSClue> {
    val clues = mutableListOf<MSClue>()
    for (row in 0 until board.rows) for (col in 0 until board.cols) {
        val cell = board.cells[row][col]
        if (cell.state == MinesweeperCellState.REVEALED && cell.adjacentMines > 0) {
            clues.add(MSClue(row, col, cell.adjacentMines))
        }
    }
    return clues
}

private fun clueState(board: MinesweeperBoard, clue: MSClue, forcedMineKeys: Set<Pair<Int, Int>>): MSClueState {
    val unknown = neighbors(board, clue.row, clue.col).filter { isUnknown(board, it) }
    val forced = unknown.filter { it in forcedMineKeys }
    val unresolved = unknown.filter { it !in forcedMineKeys }
    return MSClueState(unresolved, forced, clue.adjacentMines - forced.size)
}

private fun isSubset(subset: List<Pair<Int, Int>>, superset: List<Pair<Int, Int>>): Boolean {
    val supersetSet = superset.toSet()
    return subset.all { it in supersetSet }
}

/** Fixed-point propagation: direct-local ("all remaining neighbors of a clue must be mines") and
 * subset-difference ("a bigger clue's extra neighbors over a smaller clue's must be mines") deduction.
 * Value is true when a cell was forced via subset-difference (used to label full-clue-resolution
 * vs all-mines-accounted-for hints later, matching RN's forcedMineReasons distinction). */
private fun findForcedMineReasons(board: MinesweeperBoard, clues: List<MSClue>): Map<Pair<Int, Int>, Boolean> {
    val forced = mutableMapOf<Pair<Int, Int>, Boolean>()
    var changed = true
    while (changed) {
        changed = false
        val forcedKeys = forced.keys

        for (clue in clues) {
            val state = clueState(board, clue, forcedKeys)
            if (state.remainingMines <= 0 || state.remainingMines != state.unresolvedNeighbors.size) continue
            for (cell in state.unresolvedNeighbors) if (cell !in forced) { forced[cell] = false; changed = true }
        }

        for (i in clues.indices) {
            for (j in (i + 1) until clues.size) {
                val a = clues[i]; val b = clues[j]
                val stateA = clueState(board, a, forced.keys)
                val stateB = clueState(board, b, forced.keys)
                for ((subsetState, supersetState) in listOf(stateA to stateB, stateB to stateA)) {
                    if (subsetState.remainingMines < 0 || supersetState.remainingMines < 0 ||
                        subsetState.unresolvedNeighbors.isEmpty() || supersetState.unresolvedNeighbors.isEmpty() ||
                        !isSubset(subsetState.unresolvedNeighbors, supersetState.unresolvedNeighbors)
                    ) continue
                    val extra = supersetState.unresolvedNeighbors.filter { it !in subsetState.unresolvedNeighbors.toSet() }
                    val diff = supersetState.remainingMines - subsetState.remainingMines
                    if (diff <= 0 || diff != extra.size) continue
                    for (cell in extra) if (cell !in forced) { forced[cell] = true; changed = true }
                }
            }
        }
    }
    return forced
}

private fun canCellBeMine(board: MinesweeperBoard, candidate: Pair<Int, Int>, forcedMineKeys: Set<Pair<Int, Int>>): Boolean =
    neighbors(board, candidate.first, candidate.second).all { (r, c) ->
        val clueCell = board.cells[r][c]
        if (clueCell.state != MinesweeperCellState.REVEALED || clueCell.adjacentMines <= 0) return@all true
        val clue = MSClue(r, c, clueCell.adjacentMines)
        val state = clueState(board, clue, forcedMineKeys)
        if (candidate !in state.unresolvedNeighbors) return@all true
        val assumedMineCount = state.forcedMineNeighbors.size + 1
        if (assumedMineCount > clue.adjacentMines) return@all false
        val remainingAfter = clue.adjacentMines - assumedMineCount
        val slotsAfter = state.unresolvedNeighbors.size - 1
        remainingAfter <= slotsAfter
    }

private fun contradictionClues(board: MinesweeperBoard, candidate: Pair<Int, Int>, forcedMineKeys: Set<Pair<Int, Int>>): List<Pair<Int, Int>> =
    neighbors(board, candidate.first, candidate.second).filter { (r, c) ->
        val clueCell = board.cells[r][c]
        if (clueCell.state != MinesweeperCellState.REVEALED || clueCell.adjacentMines <= 0) return@filter false
        val clue = MSClue(r, c, clueCell.adjacentMines)
        val state = clueState(board, clue, forcedMineKeys)
        if (candidate !in state.unresolvedNeighbors) return@filter false
        val assumedMineCount = state.forcedMineNeighbors.size + 1
        if (assumedMineCount > clue.adjacentMines) return@filter true
        val remainingAfter = clue.adjacentMines - assumedMineCount
        val slotsAfter = state.unresolvedNeighbors.size - 1
        remainingAfter > slotsAfter
    }.sortedWith(::compareCells)

private fun bestByTargetCountThenClue(hints: List<MSAnalyzedHint>): MSAnalyzedHint? =
    hints.minWithOrNull(
        compareBy<MSAnalyzedHint> { it.targetCells.size }.thenComparator { a, b ->
            val ca = a.primaryClueCell; val cb = b.primaryClueCell
            if (ca == null || cb == null) 0 else compareCells(ca, cb)
        },
    )

private data class MSQuad(val subsetClue: MSClue, val supersetClue: MSClue, val subsetState: MSClueState, val supersetState: MSClueState)

private fun findOnlyOnePossibleMineHint(board: MinesweeperBoard, clues: List<MSClue>, forcedMineKeys: Set<Pair<Int, Int>>): MSAnalyzedHint? {
    val hints = mutableListOf<MSAnalyzedHint>()
    for (i in clues.indices) for (j in (i + 1) until clues.size) {
        val a = clues[i]; val b = clues[j]
        val stateA = clueState(board, a, forcedMineKeys)
        val stateB = clueState(board, b, forcedMineKeys)
        val quads = listOf(MSQuad(a, b, stateA, stateB), MSQuad(b, a, stateB, stateA))
        for (quad in quads) {
            val subsetClue = quad.subsetClue; val supersetClue = quad.supersetClue
            val subsetState = quad.subsetState; val supersetState = quad.supersetState
            if (subsetState.remainingMines != 1 || supersetState.remainingMines != 1 ||
                subsetState.unresolvedNeighbors.isEmpty() || supersetState.unresolvedNeighbors.isEmpty() ||
                !isSubset(subsetState.unresolvedNeighbors, supersetState.unresolvedNeighbors)
            ) continue
            val extra = supersetState.unresolvedNeighbors.filter { it !in subsetState.unresolvedNeighbors.toSet() }.sortedWith(::compareCells)
            if (extra.isEmpty()) continue
            hints.add(
                MSAnalyzedHint(
                    evidenceCells = dedupeCells(listOf(subsetClue.row to subsetClue.col, supersetClue.row to supersetClue.col) + subsetState.unresolvedNeighbors + supersetState.unresolvedNeighbors),
                    targetCells = extra,
                    primaryClueCell = supersetClue.row to supersetClue.col,
                    secondaryClueCell = subsetClue.row to subsetClue.col,
                    mineCount = 1,
                ),
            )
        }
    }
    return bestByTargetCountThenClue(hints)
}

private fun findGuaranteedSafeTileHint(board: MinesweeperBoard, clues: List<MSClue>, forcedMineKeys: Set<Pair<Int, Int>>): MSAnalyzedHint? {
    val candidates = dedupeCells(clues.flatMap { clueState(board, it, forcedMineKeys).unresolvedNeighbors })
    val hints = candidates.mapNotNull { candidate ->
        val contradictions = contradictionClues(board, candidate, forcedMineKeys)
        if (contradictions.isEmpty()) return@mapNotNull null
        val primary = contradictions[0]
        val primaryClue = board.cells[primary.first][primary.second]
        MSAnalyzedHint(
            evidenceCells = dedupeCells(listOf(candidate) + contradictions),
            targetCells = listOf(candidate),
            primaryClueCell = primary,
            secondaryClueCell = contradictions.getOrNull(1),
            mineCount = primaryClue.adjacentMines,
        )
    }
    return bestByTargetCountThenClue(hints)
}

private fun findSingleMineLogicHint(board: MinesweeperBoard, clues: List<MSClue>, forcedMineKeys: Set<Pair<Int, Int>>): MSAnalyzedHint? {
    val hints = clues.mapNotNull { clue ->
        val state = clueState(board, clue, forcedMineKeys)
        if (state.remainingMines != 1 || state.unresolvedNeighbors.size < 2) return@mapNotNull null
        val legalMineCandidates = state.unresolvedNeighbors.filter { canCellBeMine(board, it, forcedMineKeys) }.sortedWith(::compareCells)
        if (legalMineCandidates.size != 1) return@mapNotNull null
        val targets = state.unresolvedNeighbors.filter { it !in legalMineCandidates.toSet() }.sortedWith(::compareCells)
        if (targets.isEmpty()) return@mapNotNull null
        MSAnalyzedHint(
            evidenceCells = dedupeCells(listOf(clue.row to clue.col) + state.unresolvedNeighbors),
            targetCells = targets,
            primaryClueCell = clue.row to clue.col,
            mineCount = 1,
        )
    }
    return bestByTargetCountThenClue(hints)
}

private fun findSatisfiedClueHint(
    board: MinesweeperBoard,
    clues: List<MSClue>,
    forcedMineReasons: Map<Pair<Int, Int>, Boolean>,
    wantSubsetResolution: Boolean,
): MSAnalyzedHint? {
    val hints = clues.mapNotNull { clue ->
        val state = clueState(board, clue, forcedMineReasons.keys)
        if (state.remainingMines != 0 || state.unresolvedNeighbors.isEmpty()) return@mapNotNull null
        val usesSubsetResolution = state.forcedMineNeighbors.any { forcedMineReasons[it] == true }
        if (usesSubsetResolution != wantSubsetResolution) return@mapNotNull null
        MSAnalyzedHint(
            evidenceCells = dedupeCells(listOf(clue.row to clue.col) + state.forcedMineNeighbors),
            targetCells = state.unresolvedNeighbors.sortedWith(::compareCells),
            primaryClueCell = clue.row to clue.col,
            mineCount = clue.adjacentMines,
        )
    }
    return bestByTargetCountThenClue(hints)
}

fun getMinesweeperNextMoveHint(board: MinesweeperBoard): MinesweeperNextMoveHint {
    if (board.status != MinesweeperStatus.PLAYING) return MinesweeperGuessHint
    val clues = getClues(board)
    if (clues.isEmpty()) return MinesweeperGuessHint

    val forcedMineReasons = findForcedMineReasons(board, clues)
    val forcedMineKeys = forcedMineReasons.keys

    val analyzed = findOnlyOnePossibleMineHint(board, clues, forcedMineKeys)?.let { it to MinesweeperHintPattern.ONLY_ONE_POSSIBLE_MINE }
        ?: findGuaranteedSafeTileHint(board, clues, forcedMineKeys)?.let { it to MinesweeperHintPattern.GUARANTEED_SAFE_TILE }
        ?: findSingleMineLogicHint(board, clues, forcedMineKeys)?.let { it to MinesweeperHintPattern.SINGLE_MINE_LOGIC }
        ?: findSatisfiedClueHint(board, clues, forcedMineReasons, wantSubsetResolution = true)?.let { it to MinesweeperHintPattern.FULL_CLUE_RESOLUTION }
        ?: findSatisfiedClueHint(board, clues, forcedMineReasons, wantSubsetResolution = false)?.let { it to MinesweeperHintPattern.ALL_MINES_ACCOUNTED_FOR }

    val (hint, pattern) = analyzed ?: return MinesweeperGuessHint
    return MinesweeperPatternHint(
        pattern = pattern,
        clueCell = hint.primaryClueCell,
        secondaryClueCell = hint.secondaryClueCell,
        mineCount = hint.mineCount ?: 1,
        evidenceCells = hint.evidenceCells,
        targetCells = hint.targetCells,
    )
}
