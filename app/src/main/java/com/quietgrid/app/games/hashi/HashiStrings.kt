package com.quietgrid.app.games.hashi

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun hashiDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.hashi_difficulty_easy
    Difficulty.MEDIUM -> R.string.hashi_difficulty_medium
    Difficulty.HARD -> R.string.hashi_difficulty_hard
    Difficulty.EXPERT -> R.string.hashi_difficulty_expert
}

fun hashiDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.hashi_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.hashi_difficulty_desc_medium
    Difficulty.HARD -> R.string.hashi_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.hashi_difficulty_desc_expert
}

val HashiQuickStart = QuickStartContent(
    goalRes = R.string.hashi_quickstart_goal,
    bulletRes = listOf(
        R.string.hashi_quickstart_bullet_1,
        R.string.hashi_quickstart_bullet_2,
        R.string.hashi_quickstart_bullet_3,
    ),
    hookRes = R.string.hashi_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.hashi_rule_1_title) { HashiMatchNumbersPicture() },
        QuickRule(R.string.hashi_rule_2_title) { HashiStraightLinesPicture() },
        QuickRule(R.string.hashi_rule_3_title) { HashiNoCrossingsPicture() },
    ),
)
