package com.quietgrid.engine.wordswap

sealed interface WordSwapHint {
    data class Placement(val slotIndex: Int, val word: String, val technique: WordSwapTechnique) : WordSwapHint
    data class Swap(val move: WordSwapMove) : WordSwapHint
}

fun findWordSwapHint(puzzle: WordSwapPuzzleEntry, board: String): WordSwapHint? {
    if (board == puzzle.solution) return null
    val state = WordSwapSolverState.of(puzzle)
    wordSwapSolvedSlots(puzzle, board).sorted().forEach { slot -> state.place(slot, slot) }
    val deduction = if (!state.isSolved() && state.isFeasible()) nextWordSwapDeduction(state) else null
    if (deduction != null) return WordSwapHint.Placement(deduction.slot, state.word(deduction.word), deduction.technique)
    return findWordSwapSwapHint(board, puzzle.solution)?.let { WordSwapHint.Swap(it) }
}
