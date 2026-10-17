package com.quietgrid.app.games.nonogram

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun nonogramDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.nonogram_difficulty_easy
    Difficulty.MEDIUM -> R.string.nonogram_difficulty_medium
    Difficulty.HARD -> R.string.nonogram_difficulty_hard
    Difficulty.EXPERT -> R.string.nonogram_difficulty_expert
}

fun nonogramDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.nonogram_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.nonogram_difficulty_desc_medium
    Difficulty.HARD -> R.string.nonogram_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.nonogram_difficulty_desc_expert
}

val NonogramQuickStart = QuickStartContent(
    goalRes = R.string.nonogram_quickstart_goal,
    bulletRes = listOf(
        R.string.nonogram_quickstart_bullet_1,
        R.string.nonogram_quickstart_bullet_2,
        R.string.nonogram_quickstart_bullet_3,
    ),
    hookRes = R.string.nonogram_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.nonogram_rule_1_title) { NonogramCluePicture(clue = "3 1", cells = "###.#") },
        QuickRule(R.string.nonogram_rule_2_title) { NonogramCluePicture(clue = "4", cells = ".###.") },
        QuickRule(R.string.nonogram_rule_3_title) { NonogramCluePicture(clue = "2", cells = "##xxx") },
    ),
)
