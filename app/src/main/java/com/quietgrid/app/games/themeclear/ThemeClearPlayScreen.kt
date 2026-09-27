package com.quietgrid.app.games.themeclear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.themes.themeLabelRes
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.PuzzleLanguageFlag
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.engine.themeclear.ThemeClearSelectionState

private val SELECTION_ROW_HEIGHT: Dp = 40.dp
private val FOUND_WORD_ROW_HEIGHT: Dp = 40.dp

@Composable
fun ThemeClearPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (ThemeClearResult) -> Unit,
) {
    val viewModel = hiltViewModel<ThemeClearPlayViewModel, ThemeClearPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(viewModel.acceptEvent) {
        if (viewModel.acceptEvent > 0) haptics.correctFeedback()
    }
    LaunchedEffect(session?.puzzle?.id) {
        if (session != null) viewModel.refreshDeadEnd()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = viewModel::onHint, enabled = !viewModel.isDeadEnd) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = stringResource(
                        if (viewModel.hintTileIds.isEmpty()) R.string.themeclear_hint_show else R.string.themeclear_hint_hide,
                    ),
                    tint = if (viewModel.hintTileIds.isNotEmpty()) colors.tertiary else colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::onShuffle) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.themeclear_shuffle),
                    tint = colors.onSurfaceVariant,
                )
            }
            if (session != null) {
                PuzzleLanguageFlag(session.puzzle.locale)
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        if (session != null) {
            Text(
                stringResource(themeLabelRes(session.puzzle.themeId)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(visible = session != null, playFresh = !resume) {
            if (session != null) {
                Box(Modifier.padding(8.dp)) {
                    ThemeClearGrid(
                        session = session,
                        hintTileIds = viewModel.hintTileIds,
                        onTileTap = viewModel::onTileTap,
                    )
                }
            }
        }

        if (session != null) {
            val word = tcSelectedWord(session)
            val state = viewModel.selectionState
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(SELECTION_ROW_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                if (word.isEmpty() && viewModel.isDeadEnd) {
                    Text(
                        stringResource(R.string.themeclear_dead_end),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (state == ThemeClearSelectionState.PENDING) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = when (state) {
                            ThemeClearSelectionState.NO_MATCH -> colors.error.copy(alpha = 0.7f)
                            ThemeClearSelectionState.PENDING -> colors.primary
                            else -> colors.onSurface
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = word.isNotEmpty(), onClick = viewModel::onSelectionTextTap),
                    )
                }
            }

            val badgeListState = rememberLazyListState()
            val foundCount = session.foundWords.size
            LaunchedEffect(foundCount) {
                if (foundCount > 0) badgeListState.animateScrollToItem(foundCount - 1)
            }
            LazyRow(
                state = badgeListState,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(FOUND_WORD_ROW_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(session.foundWords) { index, found ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.secondaryContainer,
                    ) {
                        Row(
                            Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(found.word, color = colors.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { viewModel.onRemoveFoundWord(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.themeclear_remove_word, found.word),
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
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
