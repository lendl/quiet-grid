package com.quietgrid.app.games.blockfill

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun blockFillDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockfill_difficulty_easy
    Difficulty.MEDIUM -> R.string.blockfill_difficulty_medium
    Difficulty.HARD -> R.string.blockfill_difficulty_hard
    Difficulty.EXPERT -> R.string.blockfill_difficulty_expert
}

fun blockFillDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.blockfill_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.blockfill_difficulty_desc_medium
    Difficulty.HARD -> R.string.blockfill_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.blockfill_difficulty_desc_expert
}

val BlockFillQuickStart = QuickStartContent(
    goalRes = R.string.blockfill_quickstart_goal,
    bulletRes = listOf(
        R.string.blockfill_quickstart_bullet_1,
        R.string.blockfill_quickstart_bullet_2,
        R.string.blockfill_quickstart_bullet_3,
    ),
    hookRes = R.string.blockfill_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.blockfill_rule_1_title) { BlockFillLinePicture(rows = 1, family = BlockFillShapeFamily.STRAIGHT5) },
        QuickRule(R.string.blockfill_rule_2_title) { BlockFillPlanAheadPicture() },
        QuickRule(R.string.blockfill_rule_3_title) { BlockFillLinePicture(rows = 2, family = BlockFillShapeFamily.RECTANGLE, trailing = "×2") },
    ),
)
