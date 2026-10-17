package com.quietgrid.app.games.blockfill

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.CrownIcon
import com.quietgrid.app.ui.play.InfoItem

private val CROWN_SIZE = 16.dp

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

    val session = viewModel.session

    BlockFillPlayLayout(
        gameId = GameId.BLOCKFILL,
        board = session?.board,
        tray = session?.tray,
        playFresh = !resume,
        onPlacePiece = viewModel::onPlacePiece,
        timer = null,
        onBack = onBack,
        onEndPuzzle = { viewModel.endRun() },
        info = if (session == null) {
            emptyList()
        } else {
            listOf(InfoItem.Custom { BlockFillEndlessStrip(session.score, session.multiplier, session.bestAtStart) })
        },
    )
}

@Composable
private fun BlockFillEndlessStrip(score: Int, multiplier: Int, bestAtStart: Int) {
    val crowned = endlessHeaderShowsCrown(score, bestAtStart)
    val crownDescription = stringResource(R.string.blockfill_endless_crown_description)
    BlockFillStripRow {
        BlockFillScoreText(score)
        BlockFillMultiplierText(multiplier)
        if (crowned) {
            CrownIcon(
                Modifier.size(CROWN_SIZE).semantics { contentDescription = crownDescription },
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            BlockFillStripProgress(score.toFloat() / bestAtStart)
            Text(stringResource(R.string.blockfill_endless_best_value, bestAtStart))
        }
    }
}
