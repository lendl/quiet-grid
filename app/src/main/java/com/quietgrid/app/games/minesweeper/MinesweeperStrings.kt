package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun minesweeperDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.minesweeper_difficulty_easy
    Difficulty.MEDIUM -> R.string.minesweeper_difficulty_medium
    Difficulty.HARD -> R.string.minesweeper_difficulty_hard
    Difficulty.EXPERT -> R.string.minesweeper_difficulty_expert
}

fun minesweeperDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.minesweeper_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.minesweeper_difficulty_desc_medium
    Difficulty.HARD -> R.string.minesweeper_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.minesweeper_difficulty_desc_expert
}
