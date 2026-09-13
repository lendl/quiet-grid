package com.quietgrid.app.games.game2048

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

fun game2048DifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.game2048_difficulty_easy
    Difficulty.MEDIUM -> R.string.game2048_difficulty_medium
    Difficulty.HARD -> R.string.game2048_difficulty_hard
    Difficulty.EXPERT -> R.string.game2048_difficulty_expert
}

fun game2048DifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.game2048_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.game2048_difficulty_desc_medium
    Difficulty.HARD -> R.string.game2048_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.game2048_difficulty_desc_expert
}

val Game2048QuickStart = QuickStartContent(
    goalRes = R.string.game2048_quickstart_goal,
    bulletRes = listOf(
        R.string.game2048_quickstart_bullet_1,
        R.string.game2048_quickstart_bullet_2,
        R.string.game2048_quickstart_bullet_3,
    ),
    hookRes = R.string.game2048_quickstart_hook,
)
