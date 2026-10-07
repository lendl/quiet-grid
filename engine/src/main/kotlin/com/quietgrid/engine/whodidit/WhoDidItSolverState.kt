package com.quietgrid.engine.whodidit

enum class WhoDidItMark { UNKNOWN, NO, YES }

enum class WhoDidItTechnique {
    CLUE_DIRECT,
    TRAIT_DIRECT,
    SINGLE,
    TRANSITIVE,
    CLUE_PROPAGATION,
    INCOMPATIBLE,
    STATEMENT_CHECK,
    LIAR_HYPOTHESIS,
    CONTRADICTION,
}

data class WhoDidItCellChange(val a: WhoDidItRef, val b: WhoDidItRef, val mark: WhoDidItMark)

sealed interface WhoDidItSource {
    data class Clue(val index: Int) : WhoDidItSource
    data class Statement(val suspect: Int) : WhoDidItSource
}

data class WhoDidItStep(
    val technique: WhoDidItTechnique,
    val sources: List<WhoDidItSource> = emptyList(),
    val changes: List<WhoDidItCellChange> = emptyList(),
    val culpritRemovals: Set<Int> = emptySet(),
    val depth: Int = 0,
)

class WhoDidItGridState(val size: Int, val categoryCount: Int) {
    private val blocks: Array<Array<WhoDidItMark>?> = arrayOfNulls(categoryCount * categoryCount)
    val culpritCandidates: MutableSet<Int> = (0 until size).toMutableSet()

    private fun blockIndex(lo: Int, hi: Int) = lo * categoryCount + hi

    private fun cellIndex(a: WhoDidItRef, b: WhoDidItRef): Int {
        require(a.category != b.category) { "refs must be in different categories" }
        return if (a.category < b.category) a.slot * size + b.slot else b.slot * size + a.slot
    }

    fun mark(a: WhoDidItRef, b: WhoDidItRef): WhoDidItMark {
        val index = cellIndex(a, b)
        val cells = blocks[blockIndex(minOf(a.category, b.category), maxOf(a.category, b.category))]
            ?: return WhoDidItMark.UNKNOWN
        return cells[index]
    }

    fun set(a: WhoDidItRef, b: WhoDidItRef, mark: WhoDidItMark): Boolean {
        val index = cellIndex(a, b)
        val blockKey = blockIndex(minOf(a.category, b.category), maxOf(a.category, b.category))
        val cells = blocks[blockKey] ?: Array(size * size) { WhoDidItMark.UNKNOWN }.also { blocks[blockKey] = it }
        if (cells[index] == mark) return false
        cells[index] = mark
        return true
    }

    fun copy(): WhoDidItGridState {
        val clone = WhoDidItGridState(size, categoryCount)
        for (i in blocks.indices) clone.blocks[i] = blocks[i]?.copyOf()
        clone.culpritCandidates.clear()
        clone.culpritCandidates.addAll(culpritCandidates)
        return clone
    }

    fun autoFill() {
        var changed = true
        while (changed) {
            changed = false
            for (lo in 0 until categoryCount) for (hi in lo + 1 until categoryCount) {
                val cells = blocks[blockIndex(lo, hi)] ?: continue
                for (row in 0 until size) for (col in 0 until size) {
                    if (cells[row * size + col] != WhoDidItMark.YES) continue
                    for (k in 0 until size) {
                        if (k != col && cells[row * size + k] == WhoDidItMark.UNKNOWN) {
                            cells[row * size + k] = WhoDidItMark.NO
                            changed = true
                        }
                        if (k != row && cells[k * size + col] == WhoDidItMark.UNKNOWN) {
                            cells[k * size + col] = WhoDidItMark.NO
                            changed = true
                        }
                    }
                }
            }
        }
    }

    fun hasContradiction(): Boolean {
        if (culpritCandidates.isEmpty()) return true
        for (lo in 0 until categoryCount) for (hi in lo + 1 until categoryCount) {
            val cells = blocks[blockIndex(lo, hi)]
            for (line in 0 until size) {
                var rowOpen = 0
                var colOpen = 0
                var rowYes = 0
                var colYes = 0
                for (k in 0 until size) {
                    val rowMark = cells?.get(line * size + k) ?: WhoDidItMark.UNKNOWN
                    val colMark = cells?.get(k * size + line) ?: WhoDidItMark.UNKNOWN
                    if (rowMark != WhoDidItMark.NO) rowOpen++
                    if (colMark != WhoDidItMark.NO) colOpen++
                    if (rowMark == WhoDidItMark.YES) rowYes++
                    if (colMark == WhoDidItMark.YES) colYes++
                }
                if (rowOpen == 0 || colOpen == 0 || rowYes > 1 || colYes > 1) return true
            }
        }
        for (ca in 0 until categoryCount) for (cb in 0 until categoryCount) for (cc in 0 until categoryCount) {
            if (ca == cb || cb == cc || ca == cc) continue
            for (i in 0 until size) for (j in 0 until size) {
                if (mark(WhoDidItRef(ca, i), WhoDidItRef(cb, j)) != WhoDidItMark.YES) continue
                for (k in 0 until size) {
                    val viaPivot = mark(WhoDidItRef(cb, j), WhoDidItRef(cc, k))
                    val direct = mark(WhoDidItRef(ca, i), WhoDidItRef(cc, k))
                    if (viaPivot == WhoDidItMark.YES && direct == WhoDidItMark.NO) return true
                    if (viaPivot == WhoDidItMark.NO && direct == WhoDidItMark.YES) return true
                }
            }
        }
        return false
    }

    fun isSolved(): Boolean {
        if (culpritCandidates.size != 1) return false
        for (lo in 0 until categoryCount) for (hi in lo + 1 until categoryCount) {
            val cells = blocks[blockIndex(lo, hi)] ?: return false
            for (row in 0 until size) {
                if ((0 until size).count { cells[row * size + it] == WhoDidItMark.YES } != 1) return false
            }
        }
        return true
    }
}

fun WhoDidItGridState.apply(step: WhoDidItStep) {
    step.changes.forEach { set(it.a, it.b, it.mark) }
    culpritCandidates.removeAll(step.culpritRemovals)
    autoFill()
}

fun effectiveClues(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): List<Pair<WhoDidItClue, WhoDidItSource>> {
    val result = ArrayList<Pair<WhoDidItClue, WhoDidItSource>>()
    spec.clues.forEachIndexed { index, clue -> result.add(clue to WhoDidItSource.Clue(index)) }
    for (statement in spec.statements) {
        if (statement.s !in state.culpritCandidates) {
            result.add(statement.clue to WhoDidItSource.Statement(statement.s))
        } else if (state.culpritCandidates.size == 1) {
            result.add(statement.clue.negated() to WhoDidItSource.Statement(statement.s))
        }
    }
    return result
}
