package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty

const val MIN_GENUINE_USES = 2

data class SudokuGrade(val requiredStep: SudokuSkillStep, val genuineUses: Int, val difficulty: Difficulty?)

fun gradeSudoku(givens: SudokuGrid, maxStep: SudokuSkillStep = SudokuSkillStep.CHAINS_AND_COLORING): SudokuGrade? {
    if (hasDuplicateGivens(givens)) return null
    val trace = traceStrictSolve(givens, maxStep)
    if (!trace.solved) return null
    val steps = trace.moves.map { it.technique.skillStep }
    val requiredStep = steps.maxOrNull() ?: SudokuSkillStep.NAKED_SINGLES
    val genuineUses = when (requiredStep) {
        SudokuSkillStep.NAKED_SINGLES -> 0
        SudokuSkillStep.HIDDEN_SINGLES -> steps.count { it == requiredStep }
        else -> steps.indices.count { steps[it] == requiredStep && (it == 0 || steps[it - 1] != requiredStep) }
    }
    val recurs = genuineUses >= MIN_GENUINE_USES
    val difficulty = when (requiredStep) {
        SudokuSkillStep.NAKED_SINGLES -> Difficulty.EASY
        SudokuSkillStep.HIDDEN_SINGLES -> Difficulty.MEDIUM.takeIf { recurs }
        SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES -> Difficulty.HARD.takeIf { recurs }
        SudokuSkillStep.FISH_AND_WINGS -> Difficulty.EXPERT.takeIf { recurs }
        SudokuSkillStep.CHAINS_AND_COLORING -> Difficulty.EXPERT
    }
    return SudokuGrade(requiredStep, genuineUses, difficulty)
}
