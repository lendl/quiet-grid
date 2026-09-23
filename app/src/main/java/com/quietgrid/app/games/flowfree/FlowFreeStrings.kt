package com.quietgrid.app.games.flowfree

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

fun flowFreeDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.flowfree_difficulty_easy
    Difficulty.MEDIUM -> R.string.flowfree_difficulty_medium
    Difficulty.HARD -> R.string.flowfree_difficulty_hard
    Difficulty.EXPERT -> R.string.flowfree_difficulty_expert
}

fun flowFreeDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.flowfree_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.flowfree_difficulty_desc_medium
    Difficulty.HARD -> R.string.flowfree_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.flowfree_difficulty_desc_expert
}

val FlowFreeQuickStart = QuickStartContent(
    goalRes = R.string.flowfree_quickstart_goal,
    bulletRes = listOf(
        R.string.flowfree_quickstart_bullet_1,
        R.string.flowfree_quickstart_bullet_2,
        R.string.flowfree_quickstart_bullet_3,
    ),
    hookRes = R.string.flowfree_quickstart_hook,
)
