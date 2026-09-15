package com.quietgrid.app.games.starbattle

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

fun starBattleDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.starbattle_difficulty_easy
    Difficulty.MEDIUM -> R.string.starbattle_difficulty_medium
    Difficulty.HARD -> R.string.starbattle_difficulty_hard
    Difficulty.EXPERT -> R.string.starbattle_difficulty_expert
}

fun starBattleDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.starbattle_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.starbattle_difficulty_desc_medium
    Difficulty.HARD -> R.string.starbattle_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.starbattle_difficulty_desc_expert
}

val StarBattleQuickStart = QuickStartContent(
    goalRes = R.string.starbattle_quickstart_goal,
    bulletRes = listOf(
        R.string.starbattle_quickstart_bullet_1,
        R.string.starbattle_quickstart_bullet_2,
        R.string.starbattle_quickstart_bullet_3,
    ),
    hookRes = R.string.starbattle_quickstart_hook,
)
