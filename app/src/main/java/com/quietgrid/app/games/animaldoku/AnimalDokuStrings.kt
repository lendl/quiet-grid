// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuStrings.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun animalDokuDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.animaldoku_difficulty_easy
    Difficulty.MEDIUM -> R.string.animaldoku_difficulty_medium
    Difficulty.HARD -> R.string.animaldoku_difficulty_hard
    Difficulty.EXPERT -> R.string.animaldoku_difficulty_expert
}

fun animalDokuDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.animaldoku_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.animaldoku_difficulty_desc_medium
    Difficulty.HARD -> R.string.animaldoku_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.animaldoku_difficulty_desc_expert
}

val AnimalDokuQuickStart = QuickStartContent(
    goalRes = R.string.animaldoku_quickstart_goal,
    bulletRes = listOf(
        R.string.animaldoku_quickstart_bullet_1,
        R.string.animaldoku_quickstart_bullet_2,
        R.string.animaldoku_quickstart_bullet_3,
    ),
    hookRes = R.string.animaldoku_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.animaldoku_rule_1_title) { PlacementLinePicture(AnimalDokuQuickRuleMarker) },
        QuickRule(R.string.animaldoku_rule_2_title) { PlacementRegionPicture(AnimalDokuQuickRuleMarker) },
        QuickRule(R.string.animaldoku_rule_3_title) { PlacementNoTouchPicture(AnimalDokuQuickRuleMarker) },
    ),
)
