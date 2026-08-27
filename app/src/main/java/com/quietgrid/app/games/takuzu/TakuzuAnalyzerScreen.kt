package com.quietgrid.app.games.takuzu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.ui.analyzer.AnalyzerNavigationControls
import com.quietgrid.app.ui.components.GameBackButton

@Composable
fun TakuzuAnalyzerScreen(snapshot: String?, onBack: () -> Unit) {
    val viewModel = hiltViewModel<TakuzuAnalyzerViewModel, TakuzuAnalyzerViewModel.Factory>(
        creationCallback = { factory -> factory.create(snapshot) },
    )
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            GameBackButton(onBack)
            Text(
                stringResource(R.string.takuzu_analyzer_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        when (val state = viewModel.state) {
            is TakuzuAnalyzerState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is TakuzuAnalyzerState.LoadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.takuzu_analyzer_load_failed), style = MaterialTheme.typography.bodyLarge)
            }
            is TakuzuAnalyzerState.AlreadySolved -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.takuzu_analyzer_already_solved), style = MaterialTheme.typography.bodyLarge)
            }
            is TakuzuAnalyzerState.Ready -> {
                val isFinalFrame = state.currentIndex == state.steps.size
                val board = if (isFinalFrame) state.finalBoard else state.steps[state.currentIndex].boardBefore
                val hint = if (isFinalFrame) null else state.steps[state.currentIndex].hint

                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        if (hint != null) {
                            val (hintTitle, hintBody) = resolveTakuzuHintText(hint)
                            Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                            Text(hintBody, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.takuzu_analyzer_solved_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.takuzu_analyzer_solved_body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    TakuzuBoard(
                        board = board,
                        isGiven = state.isGiven,
                        finishedCells = List(state.puzzleSize) { List(state.puzzleSize) { false } },
                        feedbackCorrectRows = emptySet(),
                        feedbackCorrectCols = emptySet(),
                        feedbackIncorrectRows = emptySet(),
                        feedbackIncorrectCols = emptySet(),
                        size = state.puzzleSize,
                        onCellPress = { _, _ -> },
                        hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                        hintTargetCells = hint?.targetCells?.associate { (r, c, v) -> (r to c) to v } ?: emptyMap(),
                        hintHighlightRows = hint?.highlightRows?.toSet() ?: emptySet(),
                        hintHighlightCols = hint?.highlightCols?.toSet() ?: emptySet(),
                    )
                }

                AnalyzerNavigationControls(
                    currentIndex = state.currentIndex,
                    totalSteps = state.steps.size + 1,
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
