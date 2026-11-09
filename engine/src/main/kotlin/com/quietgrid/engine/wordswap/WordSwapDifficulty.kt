package com.quietgrid.engine.wordswap

import com.quietgrid.engine.core.Difficulty

const val WORDSWAP_EASY_MAX_SWAPS = 9
const val WORDSWAP_MEDIUM_MAX_SWAPS = 15
const val WORDSWAP_HARD_MAX_SWAPS = 20

val WORDSWAP_TIER_SIZES: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 3..4,
    Difficulty.MEDIUM to 5..5,
    Difficulty.HARD to 5..6,
    Difficulty.EXPERT to 6..6,
)

fun wordSwapSwapBand(minSwaps: Int): Difficulty = when {
    minSwaps <= WORDSWAP_EASY_MAX_SWAPS -> Difficulty.EASY
    minSwaps <= WORDSWAP_MEDIUM_MAX_SWAPS -> Difficulty.MEDIUM
    minSwaps <= WORDSWAP_HARD_MAX_SWAPS -> Difficulty.HARD
    else -> Difficulty.EXPERT
}

fun classifyWordSwapDifficulty(puzzle: WordSwapPuzzleEntry, result: WordSwapSolveResult): Difficulty? {
    if (!result.solved) return null
    val tier = wordSwapSwapBand(puzzle.minSwaps)
    return tier.takeIf { puzzle.rows in WORDSWAP_TIER_SIZES.getValue(it) }
}

fun gradeWordSwapPuzzle(puzzle: WordSwapPuzzleEntry): Difficulty? =
    classifyWordSwapDifficulty(puzzle, solveWordSwap(puzzle))

data class WordSwapSolveStep(
    val technique: WordSwapTechnique,
    val slot: Int,
    val word: String,
    val chainLength: Int,
)

data class WordSwapSolveResult(val solved: Boolean, val steps: List<WordSwapSolveStep>)

data class WordSwapSolveProfile(
    val counts: Map<WordSwapTechnique, Int>,
    val opening: WordSwapTechnique?,
    val placements: Int,
    val maxChainLength: Int,
)

fun nextWordSwapDeduction(state: WordSwapSolverState): WordSwapDeduction? =
    findWordSwapLengthSingle(state)
        ?: findWordSwapCrossingSingle(state)
        ?: findWordSwapOnlySlot(state)
        ?: findWordSwapCrossCheck(state)

fun solveWordSwap(puzzle: WordSwapPuzzleEntry): WordSwapSolveResult {
    val state = WordSwapSolverState.of(puzzle)
    val steps = mutableListOf<WordSwapSolveStep>()
    while (!state.isSolved()) {
        if (!state.isFeasible()) return WordSwapSolveResult(false, steps)
        val deduction = nextWordSwapDeduction(state) ?: return WordSwapSolveResult(false, steps)
        state.place(deduction.slot, deduction.word)
        steps += WordSwapSolveStep(deduction.technique, deduction.slot, state.word(deduction.word), deduction.chainLength)
    }
    return WordSwapSolveResult(true, steps)
}

fun analyzeWordSwapSolveResult(result: WordSwapSolveResult): WordSwapSolveProfile = WordSwapSolveProfile(
    counts = result.steps.groupingBy { it.technique }.eachCount(),
    opening = result.steps.firstOrNull()?.technique,
    placements = result.steps.size,
    maxChainLength = result.steps.maxOfOrNull { it.chainLength } ?: 0,
)
