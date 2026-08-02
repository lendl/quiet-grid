package com.quietgrid.app.games.blockfill

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun blockFillDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockfill_difficulty_easy
    Difficulty.MEDIUM -> R.string.blockfill_difficulty_medium
    Difficulty.HARD -> R.string.blockfill_difficulty_hard
    Difficulty.EXPERT -> R.string.blockfill_difficulty_expert
}

fun blockFillDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockfill_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.blockfill_difficulty_desc_medium
    Difficulty.HARD -> R.string.blockfill_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.blockfill_difficulty_desc_expert
}
