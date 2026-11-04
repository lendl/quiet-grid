package com.quietgrid.engine.sudoku

import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

fun sudokuRawScore(entry: SudokuPuzzleEntry): Long {
    val grade = gradeSudoku(entry.givens)
    val steps = traceStrictSolve(entry.givens).moves.map { it.technique.skillStep }
    val fine = packRawScore(
        (grade?.requiredStep ?: SudokuSkillStep.CHAINS_AND_COLORING).ordinal to 1,
        (grade?.genuineUses ?: 0) to 2,
        steps.count { it >= SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES } to 3,
        steps.count { it == SudokuSkillStep.HIDDEN_SINGLES } to 3,
        entry.givens.sumOf { row -> row.count { it == null } } to 2,
        steps.size to 3,
    )
    return tieredRawScore(entry.difficulty, fine)
}

fun rateSudoku(entry: SudokuPuzzleEntry, scale: JourneyRatingScale): Int = scale.rate(sudokuRawScore(entry))
