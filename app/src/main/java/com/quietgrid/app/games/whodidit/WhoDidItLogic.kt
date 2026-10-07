package com.quietgrid.app.games.whodidit

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.whodidit.WhoDidItGridState
import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.paired
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs

data class WhoDidItTapResult(val session: WhoDidItSession, val wrongPick: Boolean)

fun whoDidItChecksPicks(difficulty: Difficulty): Boolean =
    difficulty == Difficulty.EASY || difficulty == Difficulty.MEDIUM

fun whoDidItScore(elapsedSeconds: Int): Int = maxOf(0, 1000 - elapsedSeconds * 2)

fun whoDidItCellFor(categoryCount: Int, a: WhoDidItRef, b: WhoDidItRef): Triple<Int, Int, Int> {
    val pairs = whoDidItCategoryPairs(categoryCount)
    val direct = pairs.indexOf(a.category to b.category)
    if (direct >= 0) return Triple(direct, a.slot, b.slot)
    val swapped = pairs.indexOf(b.category to a.category)
    require(swapped >= 0) { "no block for categories ${a.category}, ${b.category}" }
    return Triple(swapped, b.slot, a.slot)
}

private fun cellRefs(puzzle: WhoDidItPuzzleEntry, block: Int, row: Int, col: Int): Pair<WhoDidItRef, WhoDidItRef> {
    val (rowCategory, colCategory) = whoDidItCategoryPairs(puzzle.categories.size)[block]
    return WhoDidItRef(rowCategory, row) to WhoDidItRef(colCategory, col)
}

private fun isPairedCell(puzzle: WhoDidItPuzzleEntry, block: Int, row: Int, col: Int): Boolean {
    val (a, b) = cellRefs(puzzle, block, row, col)
    return puzzle.paired(a, b)
}

private fun recomputeAutoX(cells: List<List<WhoDidItCellMark>>): List<List<WhoDidItCellMark>> {
    val size = cells.size
    val rowHasO = List(size) { row -> cells[row].any { it == WhoDidItCellMark.O } }
    val colHasO = List(size) { col -> (0 until size).any { cells[it][col] == WhoDidItCellMark.O } }
    return List(size) { row ->
        List(size) { col ->
            when (val mark = cells[row][col]) {
                WhoDidItCellMark.O, WhoDidItCellMark.X -> mark
                else -> if (rowHasO[row] || colHasO[col]) WhoDidItCellMark.AUTO_X else WhoDidItCellMark.EMPTY
            }
        }
    }
}

fun isWhoDidItBoardComplete(session: WhoDidItSession): Boolean =
    session.blocks.all { block -> block.sumOf { row -> row.count { it == WhoDidItCellMark.O } } == session.puzzle.size }

fun isWhoDidItSolved(session: WhoDidItSession): Boolean {
    if (!isWhoDidItBoardComplete(session)) return false
    return session.blocks.indices.all { b ->
        session.blocks[b].indices.all { r ->
            session.blocks[b][r].indices.all { c ->
                session.blocks[b][r][c] != WhoDidItCellMark.O || isPairedCell(session.puzzle, b, r, c)
            }
        }
    }
}

fun whoDidItShowsNotQuite(session: WhoDidItSession): Boolean =
    !whoDidItChecksPicks(Difficulty.fromKey(session.puzzle.difficulty)) &&
        isWhoDidItBoardComplete(session) && !isWhoDidItSolved(session)

fun hasWhoDidItWrongMarks(session: WhoDidItSession): Boolean =
    session.blocks.indices.any { b ->
        session.blocks[b].indices.any { r ->
            session.blocks[b][r].indices.any { c ->
                val paired = isPairedCell(session.puzzle, b, r, c)
                when (session.blocks[b][r][c]) {
                    WhoDidItCellMark.O -> !paired
                    WhoDidItCellMark.X -> paired
                    else -> false
                }
            }
        }
    }

fun applyWhoDidItTap(
    session: WhoDidItSession,
    block: Int,
    row: Int,
    col: Int,
    checkPicks: Boolean,
): WhoDidItTapResult? {
    val cells = session.blocks[block]
    val next = when (cells[row][col]) {
        WhoDidItCellMark.AUTO_X -> return null
        WhoDidItCellMark.EMPTY -> WhoDidItCellMark.X
        WhoDidItCellMark.O -> WhoDidItCellMark.EMPTY
        WhoDidItCellMark.X -> {
            val lineHasO = cells[row].any { it == WhoDidItCellMark.O } ||
                cells.indices.any { cells[it][col] == WhoDidItCellMark.O }
            if (lineHasO) WhoDidItCellMark.EMPTY else WhoDidItCellMark.O
        }
    }
    if (next == WhoDidItCellMark.O && checkPicks && !isPairedCell(session.puzzle, block, row, col)) {
        return WhoDidItTapResult(session, wrongPick = true)
    }
    val edited = List(cells.size) { r ->
        List(cells.size) { c -> if (r == row && c == col) next else cells[r][c] }
    }
    val blocks = session.blocks.mapIndexed { index, existing ->
        if (index == block) recomputeAutoX(edited) else existing
    }
    val updated = session.copy(blocks = blocks)
    val status = if (isWhoDidItSolved(updated)) WhoDidItStatus.WON else updated.status
    return WhoDidItTapResult(updated.copy(status = status), wrongPick = false)
}

fun toWhoDidItGridState(session: WhoDidItSession): WhoDidItGridState {
    val puzzle = session.puzzle
    val state = WhoDidItGridState(puzzle.size, puzzle.categories.size)
    session.blocks.forEachIndexed { b, block ->
        block.forEachIndexed { r, cells ->
            cells.forEachIndexed { c, mark ->
                val paired = isPairedCell(puzzle, b, r, c)
                val (a, other) = cellRefs(puzzle, b, r, c)
                when {
                    mark == WhoDidItCellMark.O && paired -> state.set(a, other, WhoDidItMark.YES)
                    (mark == WhoDidItCellMark.X || mark == WhoDidItCellMark.AUTO_X) && !paired ->
                        state.set(a, other, WhoDidItMark.NO)
                }
            }
        }
    }
    state.autoFill()
    return state
}
