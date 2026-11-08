package com.quietgrid.app.games.blockescape

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun blockEscapeDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockescape_difficulty_easy
    Difficulty.MEDIUM -> R.string.blockescape_difficulty_medium
    Difficulty.HARD -> R.string.blockescape_difficulty_hard
    Difficulty.EXPERT -> R.string.blockescape_difficulty_expert
}

fun blockEscapeDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockescape_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.blockescape_difficulty_desc_medium
    Difficulty.HARD -> R.string.blockescape_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.blockescape_difficulty_desc_expert
}

val BlockEscapeQuickStart = QuickStartContent(
    goalRes = R.string.blockescape_quickstart_goal,
    bulletRes = listOf(
        R.string.blockescape_quickstart_bullet_1,
        R.string.blockescape_quickstart_bullet_2,
        R.string.blockescape_quickstart_bullet_3,
    ),
    hookRes = R.string.blockescape_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.blockescape_rule_1_title) { BlockEscapeSlideAlongPicture() },
        QuickRule(R.string.blockescape_rule_2_title) { BlockEscapeNoPassingPicture() },
        QuickRule(R.string.blockescape_rule_3_title) { BlockEscapeExitPicture() },
    ),
)
