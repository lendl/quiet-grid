package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberPuzzleViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (WordSearchResult) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val viewModel = rememberPuzzleViewModel {
        WordSearchPlayViewModel(
            appContext = context,
            sessionRepository = AppContainer.sessionRepository,
            statsRepository = AppContainer.statsRepository,
            requestedDifficulty = difficulty,
            resume = resume,
        )
    }

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    var isBoardZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableStateOf(0) }
    val session = viewModel.session

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    val displayDifficulty = Difficulty.fromKey(session.puzzle.difficulty)
                    Column {
                        Text(stringResource(R.string.wordsearch_found_label), style = MaterialTheme.typography.labelSmall)
                        Text("${session.foundWordIds.size} / ${session.puzzle.words.size}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (!session.hiddenWordSolved) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.wordsearch_theme_label), style = MaterialTheme.typography.labelSmall)
                            Text(wordSearchThemeLabel(session.puzzle.hiddenWord.clue), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.wordsearch_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(wordSearchDifficultyLabelRes(displayDifficulty)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (isBoardZoomed) {
                    IconButton(onClick = { resetZoomTrigger++ }) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOutMap,
                            contentDescription = stringResource(R.string.wordsearch_reset_zoom),
                        )
                    }
                }
                if (session != null && !session.hiddenWordSolved) {
                    IconButton(onClick = { viewModel.onToggleHiddenWordMode() }) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = stringResource(
                                if (session.hiddenWordMode) R.string.wordsearch_hidden_word_exit_mode else R.string.wordsearch_hidden_word_enter_mode,
                            ),
                            tint = if (session.hiddenWordMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { viewModel.toggleNextMoveHint() }) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (viewModel.nextMoveHint != null) R.string.wordsearch_next_move_hide else R.string.wordsearch_next_move_show,
                        ),
                        tint = if (viewModel.nextMoveHint != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showEndDialog = true }) {
                    Text(stringResource(R.string.common_end_puzzle))
                }
            }
        }

        val hint = viewModel.nextMoveHint
        if (hint != null) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val title = when (hint) {
                        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_title, hint.word)
                        is WSNextMoveHint.FindHiddenLetter -> stringResource(R.string.wordsearch_hidden_word_next_letter_title, hint.clue)
                    }
                    val body = when (hint) {
                        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_body)
                        is WSNextMoveHint.FindHiddenLetter -> stringResource(R.string.wordsearch_hidden_word_next_letter_body)
                    }
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(body, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (session != null && !session.hiddenWordSolved) {
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val progressText = session.puzzle.hiddenWord.word.indices.joinToString(" ") { index ->
                        val cell = session.hiddenWordProgress.getOrNull(index)
                        if (cell != null) session.puzzle.grid[cell.row][cell.col] else "_"
                    }
                    Text(progressText, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(
                            if (session.hiddenWordMode) R.string.wordsearch_hidden_word_instructions else R.string.wordsearch_hidden_word_locked,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (session.hiddenWordMode) {
                        Text(
                            stringResource(R.string.wordsearch_hidden_word_reset_on_mistake),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        PuzzleBoardContainer(
            visible = session != null,
            playFresh = !resume,
            onZoomChange = { isBoardZoomed = it },
            resetTrigger = resetZoomTrigger,
        ) {
            if (session != null) {
                Box(Modifier.fillMaxSize().padding(8.dp)) {
                    WordSearchGrid(
                        puzzle = session.puzzle,
                        foundWordIds = session.foundWordIds,
                        tempSelection = session.tempSelection,
                        hiddenWordMode = session.hiddenWordMode,
                        hiddenWordProgress = session.hiddenWordProgress,
                        onCellTap = viewModel::onCellTap,
                        onHiddenWordTap = viewModel::onHiddenWordCellTap,
                        nextMoveEvidenceCells = hint?.let {
                            when (it) {
                                is WSNextMoveHint.FindWord -> it.evidenceCells
                                is WSNextMoveHint.FindHiddenLetter -> it.evidenceCells
                            }
                        } ?: emptyList(),
                        nextMoveTargetCells = hint?.let {
                            when (it) {
                                is WSNextMoveHint.FindWord -> it.targetCells
                                is WSNextMoveHint.FindHiddenLetter -> it.targetCells
                            }
                        } ?: emptyList(),
                    )
                }
            }
        }

        if (session != null) {
            val sortedWords = session.puzzle.words.sortedBy { it.word }
            val normalColor = MaterialTheme.colorScheme.onSurface
            val foundColor = MaterialTheme.colorScheme.onSurfaceVariant
            val separatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            val wordListText = buildAnnotatedString {
                sortedWords.forEachIndexed { index, word ->
                    val found = word.id in session.foundWordIds
                    withStyle(
                        SpanStyle(
                            color = if (found) foundColor else normalColor,
                            textDecoration = if (found) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(word.word)
                    }
                    if (index < sortedWords.size - 1) {
                        withStyle(SpanStyle(color = separatorColor)) {
                            append(" · ")
                        }
                    }
                }
            }
            Text(
                wordListText,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endPuzzle()
        },
    )
}
