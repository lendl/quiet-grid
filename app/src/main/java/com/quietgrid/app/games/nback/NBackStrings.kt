package com.quietgrid.app.games.nback

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun nbackDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.nback_difficulty_easy
    Difficulty.MEDIUM -> R.string.nback_difficulty_medium
    Difficulty.HARD -> R.string.nback_difficulty_hard
    Difficulty.EXPERT -> R.string.nback_difficulty_expert
}

fun nbackDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.nback_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.nback_difficulty_desc_medium
    Difficulty.HARD -> R.string.nback_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.nback_difficulty_desc_expert
}

val NBackQuickStart = QuickStartContent(
    goalRes = R.string.nback_quickstart_goal,
    bulletRes = listOf(
        R.string.nback_quickstart_bullet_1,
        R.string.nback_quickstart_bullet_2,
        R.string.nback_quickstart_bullet_3,
    ),
    hookRes = R.string.nback_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.nback_rule_1_title) { NBackOneSquarePicture() },
        QuickRule(R.string.nback_rule_2_title) { NBackStepsBackPicture() },
        QuickRule(R.string.nback_rule_3_title) { NBackMatchPicture() },
    ),
)
