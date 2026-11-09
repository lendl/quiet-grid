package com.quietgrid.app.games.wordswap

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun wordSwapDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordswap_difficulty_easy
    Difficulty.MEDIUM -> R.string.wordswap_difficulty_medium
    Difficulty.HARD -> R.string.wordswap_difficulty_hard
    Difficulty.EXPERT -> R.string.wordswap_difficulty_expert
}

fun wordSwapDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordswap_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.wordswap_difficulty_desc_medium
    Difficulty.HARD -> R.string.wordswap_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.wordswap_difficulty_desc_expert
}

val WordSwapQuickStart = QuickStartContent(
    goalRes = R.string.wordswap_quickstart_goal,
    bulletRes = listOf(
        R.string.wordswap_quickstart_bullet_1,
        R.string.wordswap_quickstart_bullet_2,
        R.string.wordswap_quickstart_bullet_3,
    ),
    hookRes = R.string.wordswap_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.wordswap_rule_1_title) { WordSwapSwapPicture() },
        QuickRule(R.string.wordswap_rule_2_title) { WordSwapCrossingPicture() },
        QuickRule(R.string.wordswap_rule_3_title) { WordSwapFewestPicture() },
    ),
)
