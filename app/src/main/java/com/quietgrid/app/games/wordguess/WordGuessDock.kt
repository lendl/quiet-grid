package com.quietgrid.app.games.wordguess

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.engine.wordguess.LetterState

private val MESSAGE_LINE_HEIGHT = 28.dp
private val KEYBOARD_HEIGHT = 164.dp
private val QUICK_RULE_TILE_SIZE = 18.dp

@Suppress("UNUSED_PARAMETER")
internal fun wordGuessDockHeight(sizeClass: PlaySizeClass): Dp = MESSAGE_LINE_HEIGHT + KEYBOARD_HEIGHT

@Composable
internal fun WordGuessDock(
    revealedWord: String?,
    invalidFlash: Boolean,
    keyboard: (@Composable () -> Unit)?,
    @StringRes invalidMessageRes: Int = R.string.wordguess_invalid_word_message,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(MESSAGE_LINE_HEIGHT), contentAlignment = Alignment.Center) {
            when {
                revealedWord != null -> Text(
                    "${stringResource(R.string.wordguess_reveal_word_label)}: ${revealedWord.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                invalidFlash -> FeedbackText(
                    text = stringResource(invalidMessageRes),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
                    isCorrect = false,
                    isIncorrect = true,
                )
            }
        }
        keyboard?.invoke()
    }
}

@Composable
internal fun WordGuessQuickRulePicture(word: String, highlightIndex: Int, highlightState: LetterState) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        word.forEachIndexed { index, letter ->
            WordGuessTile(
                letter = letter,
                state = if (index == highlightIndex) highlightState else null,
                isActiveRow = false,
                animateOnReveal = false,
                revealDelayMs = 0L,
                tileSize = QUICK_RULE_TILE_SIZE,
            )
        }
    }
}
