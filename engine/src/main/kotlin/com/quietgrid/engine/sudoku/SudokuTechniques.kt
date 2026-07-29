package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty

enum class SudokuTechnique {
    NAKED_SINGLE, HIDDEN_SINGLE, NAKED_PAIR, HIDDEN_PAIR, POINTING_PAIR_TRIPLE, BOX_LINE_REDUCTION,
    X_WING, SWORDFISH, XY_WING, XYZ_WING, COLORING, CHAINS,
}

val sudokuTechniqueDifficultyFloor: Map<SudokuTechnique, Difficulty> = mapOf(
    SudokuTechnique.NAKED_SINGLE to Difficulty.EASY,
    SudokuTechnique.HIDDEN_SINGLE to Difficulty.EASY,
    SudokuTechnique.NAKED_PAIR to Difficulty.EASY,
    SudokuTechnique.HIDDEN_PAIR to Difficulty.MEDIUM,
    SudokuTechnique.POINTING_PAIR_TRIPLE to Difficulty.MEDIUM,
    SudokuTechnique.BOX_LINE_REDUCTION to Difficulty.MEDIUM,
    SudokuTechnique.X_WING to Difficulty.HARD,
    SudokuTechnique.SWORDFISH to Difficulty.HARD,
    SudokuTechnique.XY_WING to Difficulty.HARD,
    SudokuTechnique.XYZ_WING to Difficulty.EXPERT,
    SudokuTechnique.COLORING to Difficulty.EXPERT,
    SudokuTechnique.CHAINS to Difficulty.EXPERT,
)

fun compareTechniques(a: SudokuTechnique, b: SudokuTechnique): Int = a.ordinal - b.ordinal

fun getHardestTechnique(techniques: List<SudokuTechnique>): SudokuTechnique? =
    techniques.maxByOrNull { it.ordinal }
