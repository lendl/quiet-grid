package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun wordSearchDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordsearch_difficulty_easy
    Difficulty.MEDIUM -> R.string.wordsearch_difficulty_medium
    Difficulty.HARD -> R.string.wordsearch_difficulty_hard
    Difficulty.EXPERT -> R.string.wordsearch_difficulty_expert
}

fun wordSearchDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordsearch_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.wordsearch_difficulty_desc_medium
    Difficulty.HARD -> R.string.wordsearch_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.wordsearch_difficulty_desc_expert
}
