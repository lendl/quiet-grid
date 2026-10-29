package com.quietgrid.app.ui.analyzer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.LogoGridLoader

private val SectionGap = 12.dp

@Composable
fun <Puzzle, Board, Hint> AnalyzerScreenFrame(
    viewModel: AnalyzerViewModel<Puzzle, Board, Hint>,
    hintText: @Composable (hint: Hint, detailIndex: Int?) -> Pair<String, String>,
    board: @Composable (puzzle: Puzzle, board: Board, hint: Hint?, detailIndex: Int?) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        when (val state = viewModel.state) {
            is AnalyzerState.Loading -> CenteredContent { LogoGridLoader() }
            is AnalyzerState.LoadFailed -> CenteredContent {
                Text(stringResource(R.string.analyzer_load_failed), style = MaterialTheme.typography.bodyLarge)
            }
            is AnalyzerState.AlreadySolved -> CenteredContent {
                Text(stringResource(R.string.analyzer_already_solved), style = MaterialTheme.typography.bodyLarge)
            }
            is AnalyzerState.Ready -> ReadyContent(state, viewModel, hintText, board)
        }
    }
}

@Composable
private fun <Puzzle, Board, Hint> ReadyContent(
    state: AnalyzerState.Ready<Puzzle, Board, Hint>,
    viewModel: AnalyzerViewModel<Puzzle, Board, Hint>,
    hintText: @Composable (hint: Hint, detailIndex: Int?) -> Pair<String, String>,
    board: @Composable (puzzle: Puzzle, board: Board, hint: Hint?, detailIndex: Int?) -> Unit,
) {
    val hint = state.hint
    val detailIndex = state.detailIndex
    val explanationScroll = remember(state.currentIndex, detailIndex) { ScrollState(0) }

    val boardSlot: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            board(state.replay.puzzle, state.board, hint, detailIndex)
        }
    }
    val explanationSlot: @Composable (Modifier) -> Unit = { modifier ->
        ExplanationCard(
            hint = hint,
            detailIndex = detailIndex,
            detailFrameCount = hint?.let { viewModel.detailFrameCount(it) } ?: 0,
            scrollState = explanationScroll,
            hintText = hintText,
            onShowDetail = viewModel::showDetail,
            onHideDetail = viewModel::hideDetail,
            modifier = modifier,
        )
    }
    val controlsSlot: @Composable () -> Unit = {
        AnalyzerNavigationControls(
            currentIndex = state.currentIndex,
            totalSteps = state.replay.steps.size + 1,
            canGoBack = state.currentIndex > 0 || detailIndex != null,
            isPlaying = state.isPlaying,
            onBack = viewModel::back,
            onNext = viewModel::next,
            onTogglePlay = viewModel::togglePlay,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePanelWidth = (maxWidth * 0.42f).coerceIn(280.dp, 420.dp)
        val cardHeight = (maxHeight * 0.26f).coerceIn(128.dp, 220.dp)
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                boardSlot(Modifier.weight(1f).fillMaxHeight())
                Column(
                    Modifier.width(sidePanelWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(SectionGap),
                ) {
                    explanationSlot(Modifier.weight(1f).fillMaxWidth())
                    controlsSlot()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                boardSlot(Modifier.weight(1f).fillMaxWidth())
                explanationSlot(Modifier.fillMaxWidth().height(cardHeight))
                controlsSlot()
            }
        }
    }
}

@Composable
private fun <Hint> ExplanationCard(
    hint: Hint?,
    detailIndex: Int?,
    detailFrameCount: Int,
    scrollState: ScrollState,
    hintText: @Composable (hint: Hint, detailIndex: Int?) -> Pair<String, String>,
    onShowDetail: () -> Unit,
    onHideDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(top = 4.dp)) {
                val (title, body) = if (hint != null) {
                    hintText(hint, detailIndex)
                } else {
                    stringResource(R.string.analyzer_solved_title) to stringResource(R.string.analyzer_solved_body)
                }
                if (detailIndex != null) {
                    Text(
                        stringResource(R.string.analyzer_detail_progress, detailIndex + 1, detailFrameCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            if (detailFrameCount > 0) {
                TextButton(
                    onClick = if (detailIndex == null) onShowDetail else onHideDetail,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(stringResource(if (detailIndex == null) R.string.analyzer_show_steps else R.string.analyzer_hide_steps))
                }
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
