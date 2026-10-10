package com.quietgrid.app.games.hashi

import com.quietgrid.engine.hashi.HASHI_MAX_BRIDGES_PER_EDGE
import com.quietgrid.engine.hashi.HashiBoard
import com.quietgrid.engine.hashi.findHashiHint
import com.quietgrid.engine.hashi.hashiBridgesCross
import com.quietgrid.engine.hashi.hashiIslandTotals
import com.quietgrid.engine.hashi.isHashiSolved
import kotlin.math.abs

enum class HashiIslandState { OPEN, COMPLETE, OVERFULL }

sealed interface HashiTapOutcome {
    data class Changed(val session: HashiSession) : HashiTapOutcome
    data object Blocked : HashiTapOutcome
}

fun applyHashiTap(session: HashiSession, edge: Int): HashiTapOutcome? {
    if (session.status != HashiStatus.PLAYING) return null
    val counts = session.bridges.toIntArray()
    val next = (counts[edge] + 1) % (HASHI_MAX_BRIDGES_PER_EDGE + 1)
    if (counts[edge] == 0 && hashiBridgesCross(session.board, counts, edge)) return HashiTapOutcome.Blocked
    counts[edge] = next
    val solved = isHashiSolved(session.board, counts)
    return HashiTapOutcome.Changed(
        session.copy(
            bridges = counts.toList(),
            highlight = null,
            status = if (solved) HashiStatus.WON else HashiStatus.PLAYING,
        ),
    )
}

fun applyHashiHint(session: HashiSession): HashiSession? {
    if (session.status != HashiStatus.PLAYING) return null
    val hint = findHashiHint(session.puzzle, session.bridges.toIntArray()) ?: return null
    return session.copy(
        hintsUsed = session.hintsUsed + 1,
        highlight = HashiHighlight(hint.edge, hint.count, hint.isMistake),
    )
}

fun hashiIslandStates(session: HashiSession): List<HashiIslandState> {
    val totals = hashiIslandTotals(session.board, session.bridges.toIntArray())
    return session.board.islands.mapIndexed { index, island ->
        when {
            totals[index] > island.count -> HashiIslandState.OVERFULL
            totals[index] == island.count -> HashiIslandState.COMPLETE
            else -> HashiIslandState.OPEN
        }
    }
}

fun resolveHashiTap(board: HashiBoard, tapRow: Float, tapCol: Float, toleranceCells: Float): Int? {
    var best: Int? = null
    var bestDistance = Float.MAX_VALUE
    board.edges.forEachIndexed { index, edge ->
        val a = board.islands[edge.a]
        val b = board.islands[edge.b]
        val distance = if (edge.horizontal) {
            val start = minOf(a.col, b.col) + 0.5f
            val end = maxOf(a.col, b.col) + 0.5f
            if (tapCol <= start + 0.35f || tapCol >= end - 0.35f) return@forEachIndexed
            abs(tapRow - (a.row + 0.5f))
        } else {
            val start = minOf(a.row, b.row) + 0.5f
            val end = maxOf(a.row, b.row) + 0.5f
            if (tapRow <= start + 0.35f || tapRow >= end - 0.35f) return@forEachIndexed
            abs(tapCol - (a.col + 0.5f))
        }
        if (distance <= toleranceCells && distance < bestDistance) {
            best = index
            bestDistance = distance
        }
    }
    return best
}
