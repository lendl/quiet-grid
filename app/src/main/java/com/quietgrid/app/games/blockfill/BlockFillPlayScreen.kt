package com.quietgrid.app.games.blockfill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton

@Composable
fun BlockFillPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (BlockFillResult) -> Unit,
) {
    val viewModel = hiltViewModel<BlockFillPlayViewModel, BlockFillPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session

    BlockFillPlayLayout(
        board = session?.board,
        tray = session?.tray,
        playFresh = !resume,
        onPlacePiece = viewModel::onPlacePiece,
    ) {
        GameBackButton(onBack)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (session != null) {
                BlockFillScoreHeader(
                    score = session.score,
                    multiplier = session.multiplier,
                    target = session.puzzle.scoreTarget,
                    elapsedSeconds = viewModel.elapsedSeconds.toInt(),
                )
            }
        }
        EndPuzzleIconButton(onClick = { showEndDialog = true })
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

@Composable
private fun BlockFillScoreHeader(score: Int, multiplier: Int, target: Int, elapsedSeconds: Int) {
    val progress = if (target > 0) score.toFloat() / target else 0f
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

    BlockFillScoreRow(score = score, multiplier = multiplier, showMultiplier = multiplier > 1)
    BlockFillHeaderProgress(progress)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.blockfill_target_value, target),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryText,
        )
        ElapsedTimerText(elapsedSeconds, style = MaterialTheme.typography.labelSmall, color = secondaryText)
    }
}
