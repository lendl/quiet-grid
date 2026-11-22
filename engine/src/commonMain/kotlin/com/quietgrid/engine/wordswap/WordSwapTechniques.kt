package com.quietgrid.engine.wordswap

enum class WordSwapTechnique { LENGTH_SINGLE, CROSSING_SINGLE, ONLY_SLOT, CROSS_CHECK }

data class WordSwapDeduction(
    val technique: WordSwapTechnique,
    val slot: Int,
    val word: Int,
    val chainLength: Int = 0,
)

fun findWordSwapLengthSingle(state: WordSwapSolverState): WordSwapDeduction? {
    val open = state.openSlots()
    val unplaced = state.unplacedWords()
    for (slot in open) {
        if (open.count { other -> state.sameShape(other, slot) } != 1) continue
        val word = unplaced.singleOrNull { state.sameShape(slot, it) } ?: continue
        if (!state.fits(slot, word)) continue
        return WordSwapDeduction(WordSwapTechnique.LENGTH_SINGLE, slot, word)
    }
    return null
}

fun findWordSwapCrossingSingle(state: WordSwapSolverState): WordSwapDeduction? {
    for (slot in state.openSlots()) {
        val word = state.fittingCandidates(slot).singleOrNull() ?: continue
        return WordSwapDeduction(WordSwapTechnique.CROSSING_SINGLE, slot, word)
    }
    return null
}

fun findWordSwapOnlySlot(state: WordSwapSolverState): WordSwapDeduction? {
    val fitting = state.openSlots().associateWith { state.fittingCandidates(it) }
    for (word in state.unplacedWords()) {
        val slot = fitting.entries.singleOrNull { word in it.value }?.key ?: continue
        return WordSwapDeduction(WordSwapTechnique.ONLY_SLOT, slot, word)
    }
    return null
}

fun crossCheckEliminate(state: WordSwapSolverState): Boolean {
    var changed = false
    do {
        var pass = false
        for (slot in state.openSlots()) {
            for (word in state.fittingCandidates(slot)) {
                if (isWordSwapCandidateSupported(state, slot, word)) continue
                state.eliminate(slot, word)
                pass = true
            }
        }
        changed = changed || pass
    } while (pass)
    return changed
}

private fun isWordSwapCandidateSupported(state: WordSwapSolverState, slot: Int, word: Int): Boolean {
    val text = state.word(word)
    val cells = state.cellsOf(slot)
    return cells.indices.all { i ->
        state.crossingsAt(cells[i]).all { crossing ->
            crossing.slot == slot || state.isPlaced(crossing.slot) ||
                state.fittingCandidates(crossing.slot).any { other -> other != word && state.word(other)[crossing.position] == text[i] }
        }
    }
}

fun findWordSwapCrossCheck(state: WordSwapSolverState): WordSwapDeduction? {
    val trial = state.copy()
    if (!crossCheckEliminate(trial)) return null
    if (!trial.isFeasible()) return null
    val single = findWordSwapCrossingSingle(trial) ?: findWordSwapOnlySlot(trial) ?: return null
    return single.copy(technique = WordSwapTechnique.CROSS_CHECK)
}
