package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty

val MINI_SUDOKU_SIZE_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 4,
    Difficulty.MEDIUM to 6,
    Difficulty.HARD to 6,
    Difficulty.EXPERT to 6,
)

private fun miniTierFor(size: Int, classicTier: Difficulty?): Difficulty? = when (size) {
    4 -> classicTier?.let { Difficulty.EASY }
    6 -> when (classicTier) {
        Difficulty.EASY -> Difficulty.MEDIUM
        Difficulty.MEDIUM -> Difficulty.HARD
        Difficulty.HARD, Difficulty.EXPERT -> Difficulty.EXPERT
        null -> null
    }
    else -> null
}

fun gradeMiniSudoku(givens: SudokuGrid, maxStep: SudokuSkillStep = SudokuSkillStep.CHAINS_AND_COLORING): SudokuGrade? {
    val grade = gradeSudoku(givens, maxStep) ?: return null
    return grade.copy(difficulty = miniTierFor(givens.size, grade.difficulty))
}

fun classifyMiniSudoku(givens: SudokuGrid): Difficulty? = gradeMiniSudoku(givens)?.difficulty
