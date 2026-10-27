// app/src/main/java/com/quietgrid/app/games/arrowescape/ArrowEscapePlayScreen.kt
package com.quietgrid.app.games.arrowescape

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayTimer
import kotlinx.coroutines.delay

@Composable
fun ArrowEscapePlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (ArrowEscapeResult) -> Unit,
) {
    val viewModel = hiltViewModel<ArrowEscapePlayViewModel, ArrowEscapePlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

    var visibleBounds by remember { mutableStateOf<Rect?>(null) }
    var panTarget by remember { mutableStateOf<Offset?>(null) }
    val session = viewModel.session
    val viewport = remember(session?.puzzle) { session?.puzzle?.let(::arrowEscapeViewport) }

    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastTapEvent) {
        val event = viewModel.lastTapEvent
        if (event != null) {
            if (event.removed) {
                haptics.correctFeedback()
            } else {
                haptics.incorrectFeedback()
            }
        }
    }

    val lives = session?.lives
    var previousLives by remember { mutableStateOf<Int?>(null) }
    var lostHeart by remember { mutableStateOf<Int?>(null) }
    var lostHeartFilled by remember { mutableStateOf(false) }
    val lostHeartScale = remember { Animatable(1f) }
    LaunchedEffect(lives) {
        val before = previousLives
        previousLives = lives
        if (lives == null || before == null || lives >= before) {
            lostHeartFilled = false
            lostHeartScale.snapTo(1f)
            return@LaunchedEffect
        }
        lostHeart = lives
        lostHeartFilled = true
        lostHeartScale.snapTo(1f)
        delay(200)
        lostHeartScale.animateTo(1.45f, animationSpec = tween(135))
        lostHeartScale.animateTo(0.8f, animationSpec = tween(315))
        lostHeartFilled = false
        lostHeartScale.animateTo(1f, animationSpec = tween(150))
    }

    GamePlayScaffold(
        gameId = GameId.ARROWESCAPE,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = viewport?.rows,
        foldCols = viewport?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        boardAspectRatio = viewport?.aspectRatio ?: 1f,
        hint = PlayHintAction(
            active = false,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(R.string.arrowescape_hint_content_description),
            onClick = viewModel::onHint,
        ),
        info = if (session == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(stringResource(R.string.arrowescape_arrows_cleared, session.removedIndices.size, session.puzzle.pieces.size)),
                InfoItem.Custom {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(ARROW_ESCAPE_STARTING_LIVES) { index ->
                            val isLostHeart = index == lostHeart
                            val filled = index < session.lives || (isLostHeart && lostHeartFilled)
                            Icon(
                                if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = LocalContentColor.current,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        val scale = if (isLostHeart) lostHeartScale.value else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    },
                            )
                        }
                    }
                },
            )
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = true,
                showFrame = false,
                panTarget = panTarget,
                onVisibleBoundsChange = { visibleBounds = it },
            ) {
                session?.let { current ->
                    ArrowEscapeGrid(
                        puzzle = current.puzzle,
                        removedIndices = current.removedIndices,
                        wrongIndices = current.wrongIndices,
                        selectedIndex = current.selectedIndex,
                        lastTap = viewModel.lastTapEvent,
                        visibleBounds = visibleBounds,
                        onRequestPan = { panTarget = it },
                        onPieceTap = viewModel::onPieceTap,
                    )
                }
            }
        },
    )
}
