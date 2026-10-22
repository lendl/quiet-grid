package com.quietgrid.app.games.themeclear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.themes.themeLabelRes
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.puzzleLanguageInfo
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
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session
    val haptics = rememberHapticController()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(viewModel.acceptEvent) {
        if (viewModel.acceptEvent > 0) haptics.correctFeedback()
    }
    LaunchedEffect(session?.puzzle?.id) {
        if (session != null) viewModel.refreshDeadEnd()
    }

    val hintActive = viewModel.hintTileIds.isNotEmpty()
    GamePlayScaffold(
        gameId = GameId.THEMECLEAR,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hintActive,
            busy = false,
            contentDescription = stringResource(if (hintActive) R.string.themeclear_hint_hide else R.string.themeclear_hint_show),
            onClick = viewModel::onHint,
            enabled = !viewModel.isDeadEnd,
        ),
        info = if (session == null) {
            emptyList()
        } else {
            listOfNotNull(
                InfoItem.Label(stringResource(themeLabelRes(session.puzzle.themeId)), emphasized = true),
                puzzleLanguageInfo(session.puzzle.locale),
            )
        },
        dockHeight = { THEME_CLEAR_DOCK_HEIGHT },
        dock = {
            if (session != null) {
                ThemeClearDock(session, viewModel)
            }
        },
        board = {
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
        },
    )
}

private val THEME_CLEAR_DOCK_HEIGHT: Dp = SELECTION_ROW_HEIGHT + FOUND_WORD_ROW_HEIGHT + 20.dp

@Composable
private fun ThemeClearDock(session: ThemeClearSession, viewModel: ThemeClearPlayViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        run {
            val word = tcSelectedWord(session)
            val state = viewModel.selectionState
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(SELECTION_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(SELECTION_ROW_HEIGHT))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                IconButton(onClick = viewModel::onShuffle, modifier = Modifier.size(SELECTION_ROW_HEIGHT)) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.themeclear_shuffle),
                        tint = colors.onSurfaceVariant,
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
}
