package com.quietgrid.app.ui.analyzer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.LogoGridLoader

@Composable
fun <Puzzle, Board, Hint> AnalyzerScreenFrame(
    viewModel: AnalyzerViewModel<Puzzle, Board, Hint>,
    onBack: () -> Unit,
    hintText: @Composable (hint: Hint, detailIndex: Int?) -> Pair<String, String>,
    board: @Composable (puzzle: Puzzle, board: Board, hint: Hint?, detailIndex: Int?) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            GameBackButton(onBack)
            Text(
                stringResource(R.string.analyzer_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        when (val state = viewModel.state) {
            is AnalyzerState.Loading -> CenteredContent { LogoGridLoader() }
            is AnalyzerState.LoadFailed -> CenteredContent {
                Text(stringResource(R.string.analyzer_load_failed), style = MaterialTheme.typography.bodyLarge)
            }
            is AnalyzerState.AlreadySolved -> CenteredContent {
                Text(stringResource(R.string.analyzer_already_solved), style = MaterialTheme.typography.bodyLarge)
            }
            is AnalyzerState.Ready -> {
                val hint = state.hint
                val detailIndex = state.detailIndex
                val detailFrameCount = hint?.let { viewModel.detailFrameCount(it) } ?: 0

                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
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
                        if (detailFrameCount > 0) {
                            TextButton(
                                onClick = if (detailIndex == null) viewModel::showDetail else viewModel::hideDetail,
                                contentPadding = PaddingValues(horizontal = 0.dp),
                            ) {
                                Text(stringResource(if (detailIndex == null) R.string.analyzer_show_steps else R.string.analyzer_hide_steps))
                            }
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    board(state.replay.puzzle, state.board, hint, detailIndex)
                }

                AnalyzerNavigationControls(
                    currentIndex = state.currentIndex,
                    totalSteps = state.replay.steps.size + 1,
                    canGoBack = state.currentIndex > 0 || detailIndex != null,
                    isPlaying = state.isPlaying,
                    onBack = viewModel::back,
                    onNext = viewModel::next,
                    onTogglePlay = viewModel::togglePlay,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
