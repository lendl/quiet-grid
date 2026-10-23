package com.quietgrid.engine.links

enum class LinksTechnique { FORCED, POCKET, LOOKAHEAD, CHAIN, DEEP_CHAIN }

data class LinksDeduction(val technique: LinksTechnique, val move: LinksMove, val chainLength: Int = 0)

fun findLinksForced(state: LinksSolverState): LinksDeduction? {
    for (color in 0 until state.colorCount) {
        if (state.isDone(color)) continue
        for (side in 0..1) {
            val moves = state.moves(color, side)
            if (moves.size == 1) return LinksDeduction(LinksTechnique.FORCED, LinksMove(color, side, moves[0]))
        }
    }
    return null
}

fun findLinksPocket(state: LinksSolverState): LinksDeduction? {
    for (cell in 0 until state.cellCount) {
        if (state.ownerOf(cell) >= 0) continue
        val open = state.neighborsOf(cell).filter { state.ownerOf(it) < 0 || state.isOpenHead(it) }
        if (open.size != 2) continue
        val head = open.firstOrNull { state.isOpenHead(it) } ?: continue
        val color = state.ownerOf(head)
        if (open.any { it != head && state.isOpenHead(it) && state.ownerOf(it) != color }) continue
        return LinksDeduction(LinksTechnique.POCKET, LinksMove(color, state.sideOfHead(head), cell))
    }
    return null
}

fun findLinksLookahead(state: LinksSolverState): LinksDeduction? {
    for (color in 0 until state.colorCount) {
        if (state.isDone(color)) continue
        for (side in 0..1) {
            val moves = state.moves(color, side)
            if (moves.size < 2) continue
            val survivors = moves.filter { cell ->
                state.copy().also { it.apply(LinksMove(color, side, cell)) }.isFeasible()
            }
            if (survivors.size == 1) return LinksDeduction(LinksTechnique.LOOKAHEAD, LinksMove(color, side, survivors[0]))
        }
    }
    return null
}

fun findLinksChain(state: LinksSolverState): LinksDeduction? = findChainDeduction(state, deep = false)

fun findLinksDeepChain(state: LinksSolverState): LinksDeduction? = findChainDeduction(state, deep = true)

private fun findChainDeduction(state: LinksSolverState, deep: Boolean): LinksDeduction? {
    val technique = if (deep) LinksTechnique.DEEP_CHAIN else LinksTechnique.CHAIN
    var best: LinksDeduction? = null
    for (color in 0 until state.colorCount) {
        if (state.isDone(color)) continue
        for (side in 0..1) {
            val moves = state.moves(color, side)
            if (moves.size < 2) continue
            var longest = 0
            val survivors = moves.filter { cell ->
                val length = linksContradictionLength(state, LinksMove(color, side, cell), deep)
                if (length != null) longest = maxOf(longest, length)
                length == null
            }
            if (survivors.size == 1 && (best == null || longest < best.chainLength)) {
                best = LinksDeduction(technique, LinksMove(color, side, survivors[0]), longest)
            }
        }
    }
    return best
}

private fun linksContradictionLength(state: LinksSolverState, move: LinksMove, deep: Boolean): Int? {
    val trial = state.copy()
    trial.apply(move)
    var length = 1
    while (true) {
        if (!trial.isFeasible()) return length
        if (trial.isSolved()) return null
        val next = findLinksForced(trial)
            ?: findLinksPocket(trial)
            ?: (if (deep) findLinksLookahead(trial) else null)
            ?: return null
        trial.apply(next.move)
        length++
    }
}
