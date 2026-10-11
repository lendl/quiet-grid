package com.quietgrid.engine.sudoku

enum class SudokuTechnique {
    NAKED_SINGLE, HIDDEN_SINGLE, NAKED_PAIR, HIDDEN_PAIR, POINTING_PAIR_TRIPLE, BOX_LINE_REDUCTION,
    X_WING, SWORDFISH, XY_WING, XYZ_WING, COLORING, CHAINS,
}

enum class SudokuSkillStep { NAKED_SINGLES, HIDDEN_SINGLES, SUBSETS_AND_LOCKED_CANDIDATES, FISH_AND_WINGS, CHAINS_AND_COLORING }

val SudokuTechnique.skillStep: SudokuSkillStep
    get() = when (this) {
        SudokuTechnique.NAKED_SINGLE -> SudokuSkillStep.NAKED_SINGLES
        SudokuTechnique.HIDDEN_SINGLE -> SudokuSkillStep.HIDDEN_SINGLES
        SudokuTechnique.NAKED_PAIR, SudokuTechnique.HIDDEN_PAIR,
        SudokuTechnique.POINTING_PAIR_TRIPLE, SudokuTechnique.BOX_LINE_REDUCTION -> SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES
        SudokuTechnique.X_WING, SudokuTechnique.SWORDFISH, SudokuTechnique.XY_WING -> SudokuSkillStep.FISH_AND_WINGS
        SudokuTechnique.XYZ_WING, SudokuTechnique.COLORING, SudokuTechnique.CHAINS -> SudokuSkillStep.CHAINS_AND_COLORING
    }
