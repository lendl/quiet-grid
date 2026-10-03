package com.quietgrid.app.games.blockfill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.CrownIcon
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton

private val CROWN_SIZE = 18.dp

fun endlessHeaderShowsCrown(score: Int, bestAtStart: Int): Boolean = bestAtStart == 0 || score > bestAtStart

@Composable
fun BlockFillEndlessPlayScreen(
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (BlockFillEndlessResult) -> Unit,
) {
    val viewModel = hiltViewModel<BlockFillEndlessViewModel, BlockFillEndlessViewModel.Factory>(
        creationCallback = { factory -> factory.create(resume) },
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
            if (session != null) BlockFillEndlessHeader(session.score, session.multiplier, session.bestAtStart)
        }
        EndPuzzleIconButton(onClick = { showEndDialog = true })
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endRun()
        },
    )
}

@Composable
private fun BlockFillEndlessHeader(score: Int, multiplier: Int, bestAtStart: Int) {
    val crowned = endlessHeaderShowsCrown(score, bestAtStart)
    val crownDescription = stringResource(R.string.blockfill_endless_crown_description)
    BlockFillScoreRow(score = score, multiplier = multiplier, showMultiplier = true) {
        if (crowned) {
            CrownIcon(
                Modifier.size(CROWN_SIZE).semantics { contentDescription = crownDescription },
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (!crowned) {
        BlockFillHeaderProgress(score.toFloat() / bestAtStart)
        Text(
            stringResource(R.string.blockfill_endless_best_value, bestAtStart),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
