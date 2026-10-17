package com.quietgrid.app.games.blockfill

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer

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

    val session = viewModel.session

    BlockFillPlayLayout(
        gameId = GameId.BLOCKFILL,
        board = session?.board,
        tray = session?.tray,
        playFresh = !resume,
        onPlacePiece = viewModel::onPlacePiece,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            val target = session.puzzle.scoreTarget
            listOf(
                InfoItem.Custom {
                    BlockFillStripRow {
                        BlockFillScoreText(session.score)
                        Text(stringResource(R.string.blockfill_target_value, target))
                        BlockFillStripProgress(if (target > 0) session.score.toFloat() / target else 0f)
                        if (session.multiplier > 1) BlockFillMultiplierText(session.multiplier)
                    }
                },
            )
        },
    )
}
