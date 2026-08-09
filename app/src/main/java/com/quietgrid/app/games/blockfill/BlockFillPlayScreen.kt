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
import kotlin.math.roundToInt

// How far above the touch point the floating drag piece hovers, so the user's thumb never
// covers the piece they're trying to place -- the on-grid ghost (BlockFillGrid) still tracks the
// raw touch point exactly, this is a purely visual lift for the piece the finger is carrying.
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

    // Live LayoutCoordinates rather than a pre-computed Offset: the board sits inside
    // PuzzleBoardContainer -> BoardEntrance, which animates in via a graphicsLayer scale, and
    // positionInRoot() is affected by ancestor layer transforms. onGloballyPositioned isn't
    // guaranteed to re-fire once that animation settles, so a snapshotted Offset can go stale by a
    // few pixels on freshly-started puzzles. Storing the coordinates object and resolving
    // positionInRoot()/size at the moment we actually need them (below) avoids that.
    var boardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Same treatment for each tray piece's own Box, keyed by tray index. BlockFillTray reports
    // this via onGloballyPositioned -- the same mechanism BlockFillGrid uses for boardCoordinates
    // above -- so both live in the same coordinate space and can be combined with drag deltas
    // below without a unit mismatch.
    var pieceCoordinates by remember { mutableStateOf(mapOf<Int, LayoutCoordinates>()) }
    var draggingPieceIndex by remember { mutableStateOf<Int?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    // Rising-edge trigger for the invalid-drop shake below, same pattern as FeedbackText's
    // isIncorrect handling elsewhere in the app.
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
    // Root-space origin of the screen's own outer Box, needed to convert dragPointer (root-space,
    // like boardOrigin above) into an offset local to that Box for the floating drag piece below.
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
    // onDragEnd below is invoked from inside BlockFillTray's pointerInput(index, piece.shapeId)
    // coroutine, which is keyed only on index/shapeId -- neither changes mid-drag, so that
    // coroutine (and the onDragEnd closure it captured when it first launched) never restarts
    // during a drag. Reading `dragPreview` directly there would read whatever it was AT THE TIME
    // the pointerInput block launched (effectively always null, since that's the pre-drag state),
    // not its value when the finger lifts. rememberUpdatedState gives onDragEnd a handle whose
    // .value is always the latest recomposition's dragPreview, regardless of when the closure
    // holding it was created.
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

            PuzzleBoardContainer(visible = currentSession != null, playFresh = !resume, zoomable = false) {
                if (currentSession != null) {
                    Box(Modifier.fillMaxSize().padding(24.dp)) {
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
                        // `startPosition` is LOCAL to this tray piece's own Box (as reported by
                        // detectDragGestures), not root-space like boardOrigin. Combine it with the
                        // piece's own root-space origin (resolved fresh here via positionInRoot() on
                        // the coordinates captured above via onPieceMeasured -- the same mechanism
                        // BlockFillGrid uses for the board) to get a root-space starting point that
                        // resolveAnchorCell can compare against boardOrigin correctly. Falling back to
                        // startPosition alone (skipping the piece origin) would misplace the drag
                        // preview relative to the finger on the very first frame -- the exact class of
                        // bug that broke the original React Native prototype's anchor tracking.
                        val pieceOrigin = pieceCoordinates[pieceIndex]?.positionInRoot() ?: Offset.Zero
                        dragPointer = pieceOrigin + startPosition
                    },
                    onDrag = { dragAmount -> dragPointer += dragAmount },
                    onDragEnd = {
                        val pieceIndex = draggingPieceIndex
                        val anchor = currentDragPreview.value?.anchor
                        when {
                            pieceIndex != null && anchor != null -> {
                                viewModel.onPlacePiece(pieceIndex, anchor.row, anchor.col)
                                draggingPieceIndex = null
                            }
                            pieceIndex != null -> rejectedDropTrigger++
                            else -> draggingPieceIndex = null
                        }
                    },
                )
            }
        }

        // The piece being dragged, rendered at board-cell scale and lifted above the raw touch
        // point so the user's thumb doesn't hide it -- this is the only always-visible indicator of
        // where the piece currently is; the on-grid ghost inside BlockFillGrid only lights up once
        // the touch point resolves to a legal anchor, which isn't true for most of a drag gesture.
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
