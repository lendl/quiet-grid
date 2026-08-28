package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartExample
import com.quietgrid.engine.wordguess.LetterState

private fun wordGuessExampleVisual(word: String, highlightIndex: Int, highlightState: LetterState): @Composable () -> Unit = {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        word.forEachIndexed { index, letter ->
            val state = if (index == highlightIndex) highlightState else null
            WordGuessTile(letter = letter, state = state, isActiveRow = false, animateOnReveal = false, revealDelayMs = 0L)
        }
    }
}

val WordGuessQuickStart = QuickStartContent(
    goalRes = R.string.wordguess_quickstart_goal,
    bulletRes = listOf(R.string.wordguess_quickstart_bullet_1, R.string.wordguess_quickstart_bullet_2),
    examples = listOf(
        QuickStartExample(
            R.string.wordguess_quickstart_example_1_word,
            R.string.wordguess_quickstart_example_1_hint,
            visual = wordGuessExampleVisual("COLOR", 2, LetterState.CORRECT),
        ),
        QuickStartExample(
            R.string.wordguess_quickstart_example_2_word,
            R.string.wordguess_quickstart_example_2_hint,
            visual = wordGuessExampleVisual("LIGHT", 1, LetterState.PRESENT),
        ),
        QuickStartExample(
            R.string.wordguess_quickstart_example_3_word,
            R.string.wordguess_quickstart_example_3_hint,
            visual = wordGuessExampleVisual("BROWN", 0, LetterState.ABSENT),
        ),
    ),
    hookRes = R.string.wordguess_quickstart_hook,
)
