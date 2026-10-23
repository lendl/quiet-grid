package com.quietgrid.app.games.links

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun linksDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.links_difficulty_easy
    Difficulty.MEDIUM -> R.string.links_difficulty_medium
    Difficulty.HARD -> R.string.links_difficulty_hard
    Difficulty.EXPERT -> R.string.links_difficulty_expert
}

fun linksDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.links_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.links_difficulty_desc_medium
    Difficulty.HARD -> R.string.links_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.links_difficulty_desc_expert
}

val LinksQuickStart = QuickStartContent(
    goalRes = R.string.links_quickstart_goal,
    bulletRes = listOf(
        R.string.links_quickstart_bullet_1,
        R.string.links_quickstart_bullet_2,
        R.string.links_quickstart_bullet_3,
    ),
    hookRes = R.string.links_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.links_rule_1_title) { LinksConnectPairsPicture() },
        QuickRule(R.string.links_rule_2_title) { LinksNoCrossingPicture() },
        QuickRule(R.string.links_rule_3_title) { LinksFillEveryCellPicture() },
    ),
)
