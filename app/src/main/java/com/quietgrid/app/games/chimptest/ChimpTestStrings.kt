package com.quietgrid.app.games.chimptest

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

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

val ChimpTestQuickStart = QuickStartContent(
    goalRes = R.string.chimp_quickstart_goal,
    bulletRes = listOf(R.string.chimp_quickstart_bullet_1, R.string.chimp_quickstart_bullet_2),
    hookRes = R.string.chimp_quickstart_hook,
)
