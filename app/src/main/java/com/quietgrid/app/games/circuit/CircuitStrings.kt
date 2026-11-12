package com.quietgrid.app.games.circuit

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.engine.circuit.CircuitHint
import com.quietgrid.engine.circuit.CircuitTechnique

fun circuitDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.circuit_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.circuit_difficulty_desc_medium
    Difficulty.HARD -> R.string.circuit_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.circuit_difficulty_desc_expert
}

fun circuitHintTitleRes(hint: CircuitHint): Int =
    if (hint.isMistake) R.string.circuit_hint_title_mistake else R.string.circuit_hint_title_turn

fun circuitHintBodyRes(hint: CircuitHint): Int = when {
    hint.isMistake -> R.string.circuit_hint_body_mistake
    else -> when (hint.technique) {
        CircuitTechnique.EDGE_MATCH -> R.string.circuit_hint_body_edge_match
        CircuitTechnique.NO_LOOP -> R.string.circuit_hint_body_no_loop
        CircuitTechnique.DEAD_END -> R.string.circuit_hint_body_dead_end
        CircuitTechnique.CONTRADICTION -> R.string.circuit_hint_body_what_if
        null -> R.string.circuit_hint_body_fallback
    }
}

val CircuitQuickStart = QuickStartContent(
    goalRes = R.string.circuit_quickstart_goal,
    bulletRes = listOf(
        R.string.circuit_quickstart_bullet_1,
        R.string.circuit_quickstart_bullet_2,
        R.string.circuit_quickstart_bullet_3,
    ),
    hookRes = R.string.circuit_quickstart_hook,
    rules = listOf(
        QuickRule(R.string.circuit_rule_1_title) { CircuitJoinPicture() },
        QuickRule(R.string.circuit_rule_2_title) { CircuitNoLooseEndsPicture() },
        QuickRule(R.string.circuit_rule_3_title) { CircuitNoLoopsPicture() },
    ),
)
