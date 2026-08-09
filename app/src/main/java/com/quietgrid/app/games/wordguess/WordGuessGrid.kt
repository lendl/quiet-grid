package com.quietgrid.app.games.wordguess

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.engine.wordguess.LetterState
import kotlinx.coroutines.delay

private const val WORD_GUESS_TILE_STAGGER_MS = 60L

@Composable
private fun WordGuessTile(letter: Char?, state: LetterState?, animateOnReveal: Boolean, revealDelayMs: Long) {
    var revealed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        when {
            state == null -> revealed = false
            !animateOnReveal -> revealed = true
            else -> {
                revealed = false
                delay(revealDelayMs)
                scale.animateTo(1.15f, animationSpec = tween(120))
                revealed = true
                scale.animateTo(1f, animationSpec = tween(120))
            }
        }
    }
    val displayState = if (revealed) state else null
    val (background, foreground) = wordGuessLetterColors(displayState)
    val icon = wordGuessLetterIcon(displayState)
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .background(background, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercaseChar().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = foreground,
            )
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
fun WordGuessGrid(
    wordLength: Int,
    maxGuesses: Int,
    guesses: List<WordGuessGuessRow>,
    currentInput: String,
    modifier: Modifier = Modifier,
) {
    val initialGuessCount = remember { guesses.size }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rowIndex in 0 until maxGuesses) {
            val submitted = guesses.getOrNull(rowIndex)
            val isCurrentRow = submitted == null && rowIndex == guesses.size
            val animateRow = rowIndex >= initialGuessCount
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (colIndex in 0 until wordLength) {
                    val letter = when {
                        submitted != null -> submitted.guess.getOrNull(colIndex)
                        isCurrentRow -> currentInput.getOrNull(colIndex)
                        else -> null
                    }
                    val state = submitted?.feedback?.getOrNull(colIndex)
                    WordGuessTile(
                        letter = letter,
                        state = state,
                        animateOnReveal = animateRow,
                        revealDelayMs = colIndex * WORD_GUESS_TILE_STAGGER_MS,
                    )
                }
            }
        }
    }
}
