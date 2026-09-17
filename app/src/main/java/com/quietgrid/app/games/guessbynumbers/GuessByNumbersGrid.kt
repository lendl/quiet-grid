package com.quietgrid.app.games.guessbynumbers

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

@Composable
fun GuessByNumbersLetterTile(letter: Char?, isActiveRow: Boolean) {
    val borderWidth = if (isActiveRow) 2.dp else 1.dp
    val borderColor = if (isActiveRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercaseChar().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun GuessByNumbersNumberChip(label: String, value: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value?.toString() ?: "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GuessByNumbersGrid(
    wordLength: Int,
    maxGuesses: Int,
    guesses: List<GuessByNumbersGuessRow>,
    currentInput: String,
    modifier: Modifier = Modifier,
) {
    val matchesLabel = stringResource(R.string.guessbynumbers_matches_label)
    val exactLabel = stringResource(R.string.guessbynumbers_exact_label)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rowIndex in 0 until maxGuesses) {
            val submitted = guesses.getOrNull(rowIndex)
            val isCurrentRow = submitted == null && rowIndex == guesses.size
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (colIndex in 0 until wordLength) {
                    val letter = when {
                        submitted != null -> submitted.guess.getOrNull(colIndex)
                        isCurrentRow -> currentInput.getOrNull(colIndex)
                        else -> null
                    }
                    GuessByNumbersLetterTile(letter = letter, isActiveRow = isCurrentRow)
                }
                GuessByNumbersNumberChip(matchesLabel, submitted?.matches)
                GuessByNumbersNumberChip(exactLabel, submitted?.exact)
            }
        }
    }
}
