package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun minesweeperDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.minesweeper_difficulty_easy
    Difficulty.MEDIUM -> R.string.minesweeper_difficulty_medium
    Difficulty.HARD -> R.string.minesweeper_difficulty_hard
    Difficulty.EXPERT -> R.string.minesweeper_difficulty_expert
}

fun minesweeperDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.minesweeper_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.minesweeper_difficulty_desc_medium
    Difficulty.HARD -> R.string.minesweeper_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.minesweeper_difficulty_desc_expert
}

val MinesweeperQuickStart = QuickStartContent(
    goalRes = R.string.minesweeper_quickstart_goal,
    bulletRes = listOf(
        R.string.minesweeper_quickstart_bullet_1,
        R.string.minesweeper_quickstart_bullet_2,
        R.string.minesweeper_quickstart_bullet_3,
    ),
    hookRes = R.string.minesweeper_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.minesweeper_rule_1_title) { MinesweeperRevealPicture() },
        QuickRule(R.string.minesweeper_rule_2_title) { MinesweeperNumbersPicture() },
        QuickRule(R.string.minesweeper_rule_3_title) { MinesweeperFlagPicture() },
    ),
)

val MinesweeperEndlessQuickStart = QuickStartContent(
    goalRes = R.string.minesweeper_endless_rules_goal,
    bulletRes = listOf(
        R.string.minesweeper_endless_rules_bullet_1,
        R.string.minesweeper_endless_rules_bullet_2,
        R.string.minesweeper_endless_rules_bullet_3,
        R.string.minesweeper_how_to_play_chord,
    ),
    hookRes = R.string.minesweeper_endless_rules_hook,
)
