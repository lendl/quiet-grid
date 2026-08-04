package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.engine.wordguess.LetterState

@Composable
private fun WordGuessTile(letter: Char?, state: LetterState?) {
    val (background, foreground) = wordGuessLetterColors(state)
    Box(
        modifier = Modifier
            .size(48.dp)
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rowIndex in 0 until maxGuesses) {
            val submitted = guesses.getOrNull(rowIndex)
            val isCurrentRow = submitted == null && rowIndex == guesses.size
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (colIndex in 0 until wordLength) {
                    val letter = when {
                        submitted != null -> submitted.guess.getOrNull(colIndex)
                        isCurrentRow -> currentInput.getOrNull(colIndex)
                        else -> null
                    }
                    val state = submitted?.feedback?.getOrNull(colIndex)
                    WordGuessTile(letter = letter, state = state)
                }
            }
        }
    }
}
