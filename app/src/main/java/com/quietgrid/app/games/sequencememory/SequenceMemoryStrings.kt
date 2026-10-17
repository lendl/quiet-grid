package com.quietgrid.app.games.sequencememory

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun sequenceMemoryDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sequencememory_difficulty_easy
    Difficulty.MEDIUM -> R.string.sequencememory_difficulty_medium
    Difficulty.HARD -> R.string.sequencememory_difficulty_hard
    Difficulty.EXPERT -> R.string.sequencememory_difficulty_expert
}

fun sequenceMemoryDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.sequencememory_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.sequencememory_difficulty_desc_medium
    Difficulty.HARD -> R.string.sequencememory_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.sequencememory_difficulty_desc_expert
}

val SequenceMemoryQuickStart = QuickStartContent(
    goalRes = R.string.sequencememory_quickstart_goal,
    bulletRes = listOf(
        R.string.sequencememory_quickstart_bullet_1,
        R.string.sequencememory_quickstart_bullet_2,
        R.string.sequencememory_quickstart_bullet_3,
    ),
    hookRes = R.string.sequencememory_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.sequencememory_rule_1_title) { SequenceWatchPicture() },
        QuickRule(R.string.sequencememory_rule_2_title) { SequenceRepeatPicture() },
        QuickRule(R.string.sequencememory_rule_3_title) { SequenceGrowPicture() },
    ),
)
