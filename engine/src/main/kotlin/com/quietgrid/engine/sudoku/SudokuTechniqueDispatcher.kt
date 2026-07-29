package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty

interface SudokuTechniqueDispatcher {
    val technique: SudokuTechnique
    val tier: Difficulty
    fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove?
}
