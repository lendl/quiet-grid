package com.quietgrid.app.games.links

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.engine.links.linksConnectedCount
import com.quietgrid.engine.links.linksFilledCount

@Composable
fun LinksPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (LinksResult) -> Unit,
) {
    val viewModel = hiltViewModel<LinksPlayViewModel, LinksPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session
    val paths = viewModel.shownPaths
    GamePlayScaffold(
        gameId = GameId.LINKS,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = false,
            busy = false,
            contentDescription = stringResource(R.string.links_hint_content_description),
            onClick = viewModel::onHint,
        ),
        info = if (session == null || paths == null) {
            emptyList()
        } else {
            val puzzle = session.puzzle
            listOf(
                InfoItem.Label(stringResource(R.string.links_pairs_done, linksConnectedCount(puzzle, paths), puzzle.pairCount)),
                InfoItem.Label(stringResource(R.string.links_moves, session.moves)),
                InfoItem.Label(stringResource(R.string.links_filled, linksFilledCount(puzzle, paths) * 100 / puzzle.cellCount)),
            )
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = false,
            ) {
                if (session != null && paths != null) {
                    LinksGrid(
                        puzzle = session.puzzle,
                        paths = paths,
                        activeColor = viewModel.stroke?.color,
                        onDragStart = viewModel::onDragStart,
                        onDragMove = viewModel::onDragMove,
                        onDragEnd = viewModel::onDragEnd,
                    )
                }
            }
        },
    )
}
