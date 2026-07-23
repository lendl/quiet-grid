package com.quietgrid.app.games.takuzu

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun takuzuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.takuzu_difficulty_easy
    Difficulty.MEDIUM -> R.string.takuzu_difficulty_medium
    Difficulty.HARD -> R.string.takuzu_difficulty_hard
    Difficulty.EXPERT -> R.string.takuzu_difficulty_expert
}

fun takuzuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.takuzu_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.takuzu_difficulty_desc_medium
    Difficulty.HARD -> R.string.takuzu_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.takuzu_difficulty_desc_expert
}
