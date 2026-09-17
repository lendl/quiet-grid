package com.quietgrid.app.games.guessbynumbers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartExample

fun guessByNumbersDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.guessbynumbers_difficulty_easy
    Difficulty.MEDIUM -> R.string.guessbynumbers_difficulty_medium
    Difficulty.HARD -> R.string.guessbynumbers_difficulty_hard
    Difficulty.EXPERT -> R.string.guessbynumbers_difficulty_expert
}

fun guessByNumbersDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.guessbynumbers_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.guessbynumbers_difficulty_desc_medium
    Difficulty.HARD -> R.string.guessbynumbers_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.guessbynumbers_difficulty_desc_expert
}

private val guessByNumbersExampleVisual: @Composable () -> Unit = {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        "wrong".forEach { letter -> GuessByNumbersLetterTile(letter = letter, isActiveRow = false) }
        GuessByNumbersNumberChip(stringResource(R.string.guessbynumbers_matches_label), 4)
        GuessByNumbersNumberChip(stringResource(R.string.guessbynumbers_exact_label), 2)
    }
}

val GuessByNumbersQuickStart = QuickStartContent(
    goalRes = R.string.guessbynumbers_quickstart_goal,
    bulletRes = listOf(R.string.guessbynumbers_quickstart_bullet_1, R.string.guessbynumbers_quickstart_bullet_2),
    examples = listOf(
        QuickStartExample(
            R.string.guessbynumbers_quickstart_example_1_word,
            R.string.guessbynumbers_quickstart_example_1_hint,
            visual = guessByNumbersExampleVisual,
        ),
    ),
    hookRes = R.string.guessbynumbers_quickstart_hook,
)
