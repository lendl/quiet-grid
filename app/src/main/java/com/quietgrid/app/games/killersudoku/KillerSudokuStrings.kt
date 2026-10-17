package com.quietgrid.app.games.killersudoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun killerSudokuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.killersudoku_difficulty_easy
    Difficulty.MEDIUM -> R.string.killersudoku_difficulty_medium
    Difficulty.HARD -> R.string.killersudoku_difficulty_hard
    Difficulty.EXPERT -> R.string.killersudoku_difficulty_expert
}

fun killerSudokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.killersudoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.killersudoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.killersudoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.killersudoku_difficulty_desc_expert
}

val KillerSudokuQuickStart = QuickStartContent(
    goalRes = R.string.killersudoku_quickstart_goal,
    bulletRes = listOf(R.string.killersudoku_quickstart_bullet_1, R.string.killersudoku_quickstart_bullet_2),
    hookRes = R.string.killersudoku_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.killersudoku_rule_1_title) { KillerClassicRulesPicture() },
        QuickRule(R.string.killersudoku_rule_2_title) { KillerCageSumPicture() },
        QuickRule(R.string.killersudoku_rule_3_title) { KillerCageRepeatPicture() },
    ),
)
