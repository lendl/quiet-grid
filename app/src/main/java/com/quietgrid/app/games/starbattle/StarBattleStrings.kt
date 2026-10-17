package com.quietgrid.app.games.starbattle

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.games.animaldoku.PlacementLinePicture
import com.quietgrid.app.games.animaldoku.PlacementMarker
import com.quietgrid.app.games.animaldoku.PlacementNoTouchPicture
import com.quietgrid.app.games.animaldoku.PlacementRegionPicture
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent

fun starBattleDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.starbattle_difficulty_easy
    Difficulty.MEDIUM -> R.string.starbattle_difficulty_medium
    Difficulty.HARD -> R.string.starbattle_difficulty_hard
    Difficulty.EXPERT -> R.string.starbattle_difficulty_expert
}

fun starBattleDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.starbattle_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.starbattle_difficulty_desc_medium
    Difficulty.HARD -> R.string.starbattle_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.starbattle_difficulty_desc_expert
}

val StarBattleQuickStart = QuickStartContent(
    goalRes = R.string.starbattle_quickstart_goal,
    bulletRes = listOf(
        R.string.starbattle_quickstart_bullet_1,
        R.string.starbattle_quickstart_bullet_2,
        R.string.starbattle_quickstart_bullet_3,
    ),
    hookRes = R.string.starbattle_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.starbattle_rule_1_title) { PlacementLinePicture(StarBattleQuickRuleMarker) },
        QuickRule(R.string.starbattle_rule_3_title) { PlacementRegionPicture(StarBattleQuickRuleMarker) },
        QuickRule(R.string.starbattle_rule_4_title) { PlacementNoTouchPicture(StarBattleQuickRuleMarker) },
    ),
)

private val StarBattleQuickRuleMarker: PlacementMarker = { modifier -> StarBattleStarIcon(modifier) }
