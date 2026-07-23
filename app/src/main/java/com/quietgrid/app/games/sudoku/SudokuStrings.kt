package com.quietgrid.app.games.sudoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun sudokuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sudoku_difficulty_easy
    Difficulty.MEDIUM -> R.string.sudoku_difficulty_medium
    Difficulty.HARD -> R.string.sudoku_difficulty_hard
    Difficulty.EXPERT -> R.string.sudoku_difficulty_expert
}

fun sudokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sudoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.sudoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.sudoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.sudoku_difficulty_desc_expert
}
