package com.quietgrid.app.games.chimptest

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun chimpDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.chimp_difficulty_easy
    Difficulty.MEDIUM -> R.string.chimp_difficulty_medium
    Difficulty.HARD -> R.string.chimp_difficulty_hard
    Difficulty.EXPERT -> R.string.chimp_difficulty_expert
}

fun chimpDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.chimp_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.chimp_difficulty_desc_medium
    Difficulty.HARD -> R.string.chimp_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.chimp_difficulty_desc_expert
}
