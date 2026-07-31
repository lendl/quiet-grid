package com.quietgrid.app.games.chimptest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberPuzzleViewModel

@Composable
fun ChimpTestPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (ChimpTestResult) -> Unit,
) {
    val viewModel = rememberPuzzleViewModel {
        ChimpTestPlayViewModel(
            sessionRepository = AppContainer.sessionRepository,
            statsRepository = AppContainer.statsRepository,
            requestedDifficulty = difficulty,
            resume = resume,
        )
    }

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }

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
                    val roundNumber = session.currentCount - session.puzzle.startCount + 1
                    val totalRounds = session.puzzle.maxCount - session.puzzle.startCount + 1
                    val displayDifficulty = Difficulty.fromKey(session.puzzle.difficulty)
                    Column {
                        Text(stringResource(R.string.chimp_round_label), style = MaterialTheme.typography.labelSmall)
                        Text("$roundNumber / $totalRounds", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.chimp_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(chimpDifficultyLabelRes(displayDifficulty)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = { showEndDialog = true }) {
                    Text(stringResource(R.string.common_end_puzzle))
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
            if (session != null) {
                Box(Modifier.fillMaxSize().padding(24.dp)) {
                    ChimpTestGrid(
                        cells = session.cells,
                        revealAll = session.revealAll,
                        wrongTapCell = session.wrongTapCell,
                        gridSize = session.puzzle.gridSize,
                        nextExpected = session.nextExpected,
                        onCellTap = viewModel::onCellTap,
                    )
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
