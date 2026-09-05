package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import kotlin.math.roundToInt

private val FLOATING_PIECE_LIFT = 72.dp

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
    val haptics = rememberHapticController()
    var flashTrigger by remember { mutableStateOf(0) }

    var boardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pieceCoordinates by remember { mutableStateOf(mapOf<Int, LayoutCoordinates>()) }
    var draggingPieceIndex by remember { mutableStateOf<Int?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    var rejectedDropTrigger by remember { mutableStateOf(0) }
    val rejectedDropShakeX = remember { Animatable(0f) }
    LaunchedEffect(rejectedDropTrigger) {
        if (rejectedDropTrigger > 0) {
            rejectedDropShakeX.snapTo(0f)
            rejectedDropShakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -10f at 60
                    10f at 120
                    -8f at 180
                    8f at 240
                    -4f at 300
                    0f at 400
                },
            )
            draggingPieceIndex = null
        }
    }
    var screenCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val currentSession = session
    val boardOrigin = boardCoordinates?.positionInRoot()
    val boardCellSizePx = boardCoordinates?.size?.width?.div(BLOCKFILL_BOARD_SIZE.toFloat())
    val dragPreview = if (
        currentSession != null &&
        draggingPieceIndex != null &&
        boardOrigin != null &&
        boardCellSizePx != null &&
        boardCellSizePx > 0f
    ) {
        val piece = currentSession.tray.getOrNull(draggingPieceIndex!!)
        if (piece != null) {
            val anchor = resolveAnchorCell(
                pointerX = dragPointer.x,
                pointerY = dragPointer.y,
                boardOriginX = boardOrigin.x,
                boardOriginY = boardOrigin.y,
                cellSizePx = boardCellSizePx,
                board = currentSession.board,
                pieceCells = piece.cells,
            )
            val cleared = if (anchor != null) {
                previewClearedCells(currentSession.board, piece.cells, anchor.row, anchor.col, piece.family).toSet()
            } else {
                emptySet()
            }
            BlockFillDragPreview(pieceCells = piece.cells, family = piece.family, anchor = anchor, clearedCells = cleared)
        } else {
            null
        }
    } else {
        null
    }
    val currentDragPreview = rememberUpdatedState(dragPreview)

    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates -> screenCoordinates = coordinates }) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GameBackButton(onBack)
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentSession != null) {
                        Column {
                            Text(stringResource(R.string.blockfill_score_label), style = MaterialTheme.typography.labelSmall)
                            Text(currentSession.score.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text(stringResource(R.string.blockfill_target_label), style = MaterialTheme.typography.labelSmall)
                            Text(currentSession.puzzle.scoreTarget.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    EndPuzzleIconButton(onClick = { showEndDialog = true })
                }
            }

            ElapsedTimerText(
                viewModel.elapsedSeconds.toInt(),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            PuzzleBoardContainer(
                visible = currentSession != null,
                playFresh = !resume,
                zoomable = false,
                flashTrigger = flashTrigger,
            ) {
                if (currentSession != null) {
                    Box(Modifier.padding(24.dp)) {
                        BlockFillGrid(
                            board = currentSession.board,
                            dragPreview = dragPreview,
                            onBoardMeasured = { coordinates -> boardCoordinates = coordinates },
                        )
                    }
                }
            }

            if (currentSession != null) {
                BlockFillTray(
                    tray = currentSession.tray,
                    draggingPieceIndex = draggingPieceIndex,
                    onPieceMeasured = { pieceIndex, coordinates ->
                        pieceCoordinates = pieceCoordinates + (pieceIndex to coordinates)
                    },
                    onDragStart = { pieceIndex, startPosition ->
                        draggingPieceIndex = pieceIndex
                        val pieceOrigin = pieceCoordinates[pieceIndex]?.positionInRoot() ?: Offset.Zero
                        dragPointer = pieceOrigin + startPosition
                    },
                    onDrag = { dragAmount -> dragPointer += dragAmount },
                    onDragEnd = {
                        val pieceIndex = draggingPieceIndex
                        val anchor = currentDragPreview.value?.anchor
                        when {
                            pieceIndex != null && anchor != null -> {
                                val placed = viewModel.onPlacePiece(pieceIndex, anchor.row, anchor.col)
                                if (placed) {
                                    haptics.correctFeedback()
                                } else {
                                    haptics.incorrectFeedback()
                                    flashTrigger++
                                }
                                draggingPieceIndex = null
                            }
                            pieceIndex != null -> rejectedDropTrigger++
                            else -> draggingPieceIndex = null
                        }
                    },
                )
            }
        }

        val floatingPiece = currentSession?.tray?.getOrNull(draggingPieceIndex ?: -1)
        val screenOrigin = screenCoordinates?.positionInRoot()
        if (floatingPiece != null && screenOrigin != null) {
            val density = LocalDensity.current
            val floatingCellSizePx = boardCellSizePx ?: with(density) { 32.dp.toPx() }
            val floatingCellSizeDp = with(density) { floatingCellSizePx.toDp() }
            val liftPx = with(density) { FLOATING_PIECE_LIFT.toPx() }
            val maxRow = floatingPiece.cells.maxOf { it.first }
            val maxCol = floatingPiece.cells.maxOf { it.second }
            val widthPx = floatingCellSizePx * (maxCol + 1)
            val heightPx = floatingCellSizePx * (maxRow + 1)

            BlockFillPieceGlyph(
                piece = floatingPiece,
                cellSize = floatingCellSizeDp,
                cellGap = 2.dp,
                modifier = Modifier
                    .alpha(0.9f)
                    .offset {
                        val localX = dragPointer.x - screenOrigin.x
                        val localY = dragPointer.y - screenOrigin.y
                        IntOffset(
                            (localX - widthPx / 2f).roundToInt(),
                            (localY - heightPx - liftPx).roundToInt(),
                        )
                    }
                    .graphicsLayer(translationX = rejectedDropShakeX.value),
            )
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
