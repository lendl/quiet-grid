package com.quietgrid.app.games.whodidit

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

fun whoDidItDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.whodidit_difficulty_easy
    Difficulty.MEDIUM -> R.string.whodidit_difficulty_medium
    Difficulty.HARD -> R.string.whodidit_difficulty_hard
    Difficulty.EXPERT -> R.string.whodidit_difficulty_expert
}

fun whoDidItDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.whodidit_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.whodidit_difficulty_desc_medium
    Difficulty.HARD -> R.string.whodidit_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.whodidit_difficulty_desc_expert
}

val WhoDidItQuickStart = QuickStartContent(
    goalRes = R.string.whodidit_quickstart_goal,
    bulletRes = listOf(
        R.string.whodidit_quickstart_bullet_1,
        R.string.whodidit_quickstart_bullet_2,
        R.string.whodidit_quickstart_bullet_3,
    ),
    hookRes = R.string.whodidit_quickstart_hook,
)
