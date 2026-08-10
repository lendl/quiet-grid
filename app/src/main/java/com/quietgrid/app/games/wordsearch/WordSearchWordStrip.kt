package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.FeedbackText

@Composable
private fun HiddenWordToggleIcon(
    session: WordSearchSession,
    onToggleHiddenWordMode: () -> Unit,
) {
    val toggleDescription = stringResource(
        if (session.hiddenWordMode) R.string.wordsearch_hidden_word_exit_mode else R.string.wordsearch_hidden_word_enter_mode,
    )
    Icon(
        imageVector = Icons.Filled.VpnKey,
        contentDescription = toggleDescription,
        tint = if (session.hiddenWordMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggleHiddenWordMode),
    )
}

@Composable
private fun WordSearchWordItems(
    session: WordSearchSession,
    wrongHiddenWordTap: Boolean,
) {
    if (session.hiddenWordMode) {
        val progressText = session.puzzle.hiddenWord.word.indices.joinToString(" ") { index ->
            val cell = session.hiddenWordProgress.getOrNull(index)
            if (cell != null) session.puzzle.grid[cell.row][cell.col] else "_"
        }
        FeedbackText(
            text = progressText,
            style = MaterialTheme.typography.titleSmall,
            isCorrect = false,
            isIncorrect = wrongHiddenWordTap,
        )
    } else {
        val sortedWords = session.puzzle.words.sortedBy { it.word }
        sortedWords.forEach { word ->
            val found = word.id in session.foundWordIds
            Text(
                word.word,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                textDecoration = if (found) TextDecoration.LineThrough else TextDecoration.None,
                color = if (found) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchWordStrip(
    session: WordSearchSession,
    onToggleHiddenWordMode: () -> Unit,
    wrongHiddenWordTap: Boolean,
    isSideColumn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isSideColumn) {
        Column(modifier) {
            if (!session.hiddenWordSolved) {
                HiddenWordToggleIcon(session, onToggleHiddenWordMode)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WordSearchWordItems(session, wrongHiddenWordTap)
            }
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!session.hiddenWordSolved) {
                HiddenWordToggleIcon(session, onToggleHiddenWordMode)
            }

            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WordSearchWordItems(session, wrongHiddenWordTap)
            }
        }
    }
}
