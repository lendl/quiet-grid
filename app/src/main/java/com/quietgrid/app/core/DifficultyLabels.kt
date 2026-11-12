package com.quietgrid.app.core

import androidx.annotation.StringRes
import com.quietgrid.app.R

@StringRes
fun standardDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}
