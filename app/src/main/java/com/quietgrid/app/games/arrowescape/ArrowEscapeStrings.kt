// app/src/main/java/com/quietgrid/app/games/arrowescape/ArrowEscapeStrings.kt
package com.quietgrid.app.games.arrowescape

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent

fun arrowEscapeDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.arrowescape_difficulty_easy
    Difficulty.MEDIUM -> R.string.arrowescape_difficulty_medium
    Difficulty.HARD -> R.string.arrowescape_difficulty_hard
    Difficulty.EXPERT -> R.string.arrowescape_difficulty_expert
}

fun arrowEscapeDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.arrowescape_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.arrowescape_difficulty_desc_medium
    Difficulty.HARD -> R.string.arrowescape_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.arrowescape_difficulty_desc_expert
}

val ArrowEscapeQuickStart = QuickStartContent(
    goalRes = R.string.arrowescape_quickstart_goal,
    bulletRes = listOf(
        R.string.arrowescape_quickstart_bullet_1,
        R.string.arrowescape_quickstart_bullet_2,
        R.string.arrowescape_quickstart_bullet_3,
    ),
    hookRes = R.string.arrowescape_quickstart_hook,
)
