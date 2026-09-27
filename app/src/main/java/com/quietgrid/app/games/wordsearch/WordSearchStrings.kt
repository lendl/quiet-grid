package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartExample

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

val WordSearchQuickStart = QuickStartContent(
    goalRes = R.string.wordsearch_quickstart_goal,
    bulletRes = listOf(R.string.wordsearch_quickstart_bullet_1, R.string.wordsearch_quickstart_bullet_2),
    examples = listOf(
        QuickStartExample(R.string.wordsearch_quickstart_example_1_word, R.string.wordsearch_quickstart_example_1_hint),
    ),
    hookRes = R.string.wordsearch_quickstart_hook,
)
