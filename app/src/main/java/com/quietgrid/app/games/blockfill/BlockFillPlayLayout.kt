package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import kotlin.math.roundToInt

private val FLOATING_PIECE_LIFT = 72.dp
private const val FLOATING_PIECE_ALPHA = 0.9f
private val HEADER_PROGRESS_WIDTH = 96.dp
private val HEADER_PROGRESS_HEIGHT = 4.dp

@Composable
fun BlockFillPlayLayout(
    board: BlockFillBoard?,
    tray: List<BlockFillPiece?>?,
    playFresh: Boolean,
    onPlacePiece: (pieceIndex: Int, anchorRow: Int, anchorCol: Int) -> Boolean,
    header: @Composable RowScope.() -> Unit,
) {
    var boardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var slotCoordinates by remember { mutableStateOf(mapOf<Int, LayoutCoordinates>()) }
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

    val density = LocalDensity.current
    val liftPx = with(density) { FLOATING_PIECE_LIFT.toPx() }
    val boardOrigin = boardCoordinates?.positionInRoot()
    val boardCellSizePx = boardCoordinates?.size?.width?.div(BLOCKFILL_BOARD_SIZE.toFloat())
    val placeablePieces = remember(board, tray) {
        if (board == null) emptyList() else tray?.map { piece -> piece != null && pieceFitsAnywhere(board, piece.cells) }.orEmpty()
    }
    val dragPreview = if (
        board != null &&
        tray != null &&
        draggingPieceIndex != null &&
        boardOrigin != null &&
        boardCellSizePx != null &&
        boardCellSizePx > 0f
    ) {
        val piece = tray.getOrNull(draggingPieceIndex!!)
        if (piece != null) {
            val pieceTopLeft = floatingPieceTopLeft(dragPointer.x, dragPointer.y, piece.cells, boardCellSizePx, liftPx)
            val anchor = resolveAnchorCell(
                pieceLeftX = pieceTopLeft.x,
                pieceTopY = pieceTopLeft.y,
                boardOriginX = boardOrigin.x,
                boardOriginY = boardOrigin.y,
                cellSizePx = boardCellSizePx,
                board = board,
                pieceCells = piece.cells,
            )
            val clearedLines = if (anchor != null) {
                previewClearedLines(board, piece.cells, anchor.row, anchor.col)
            } else {
                BlockFillClearedLines(emptySet(), emptySet())
            }
            BlockFillDragPreview(pieceCells = piece.cells, family = piece.family, anchor = anchor, clearedLines = clearedLines)
        } else {
            null
        }
    } else {
        null
    }
    val currentDragPreview = rememberUpdatedState(dragPreview)
    val currentOnPlacePiece = rememberUpdatedState(onPlacePiece)

    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates -> screenCoordinates = coordinates }) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = header,
            )

            PuzzleBoardContainer(
                visible = board != null,
                playFresh = playFresh,
                zoomable = false,
                showFrame = false,
            ) {
                if (board != null) {
                    BlockFillGrid(
                        board = board,
                        dragPreview = dragPreview,
                        onBoardMeasured = { coordinates -> boardCoordinates = coordinates },
                    )
                }
            }

            if (tray != null) {
                BlockFillTray(
                    tray = tray,
                    placeablePieces = placeablePieces,
                    draggingPieceIndex = draggingPieceIndex,
                    onSlotMeasured = { pieceIndex, coordinates ->
                        slotCoordinates = slotCoordinates + (pieceIndex to coordinates)
                    },
                    onDragStart = { pieceIndex, startPosition ->
                        draggingPieceIndex = pieceIndex
                        val slotOrigin = slotCoordinates[pieceIndex]?.positionInRoot() ?: Offset.Zero
                        dragPointer = slotOrigin + startPosition
                    },
                    onDragCancel = { draggingPieceIndex = null },
                    onDrag = { dragAmount -> dragPointer += dragAmount },
                    onDragEnd = {
                        val pieceIndex = draggingPieceIndex
                        val anchor = currentDragPreview.value?.anchor
                        when {
                            pieceIndex != null && anchor != null && currentOnPlacePiece.value(pieceIndex, anchor.row, anchor.col) ->
                                draggingPieceIndex = null
                            pieceIndex != null -> rejectedDropTrigger++
                            else -> draggingPieceIndex = null
                        }
                    },
                )
            }
        }

        val floatingPiece = tray?.getOrNull(draggingPieceIndex ?: -1)
        val screenOrigin = screenCoordinates?.positionInRoot()
        if (floatingPiece != null && screenOrigin != null) {
            val floatingCellSizePx = boardCellSizePx ?: with(density) { 32.dp.toPx() }
            val floatingCellSizeDp = with(density) { floatingCellSizePx.toDp() }
            val floatingAlpha = if (dragPreview?.anchor != null) FLOATING_PIECE_ALPHA else UNPLACEABLE_PIECE_ALPHA

            BlockFillPieceGlyph(
                piece = floatingPiece,
                cellSize = floatingCellSizeDp,
                cellGap = BLOCKFILL_BOARD_TILE_GAP,
                modifier = Modifier
                    .offset {
                        val topLeft = floatingPieceTopLeft(dragPointer.x, dragPointer.y, floatingPiece.cells, floatingCellSizePx, liftPx)
                        IntOffset(
                            (topLeft.x - screenOrigin.x).roundToInt(),
                            (topLeft.y - screenOrigin.y).roundToInt(),
                        )
                    }
                    .graphicsLayer {
                        translationX = rejectedDropShakeX.value
                        alpha = floatingAlpha
                    },
            )
        }
    }
}

@Composable
internal fun BlockFillScoreRow(score: Int, multiplier: Int, showMultiplier: Boolean, trailing: @Composable () -> Unit = {}) {
    val scoreLabel = stringResource(R.string.blockfill_score_label)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            score.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "$scoreLabel $score" },
        )
        if (showMultiplier) {
            Text(
                stringResource(R.string.blockfill_multiplier_value, multiplier),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        trailing()
    }
}

@Composable
internal fun BlockFillHeaderProgress(progress: Float) {
    val trackShape = RoundedCornerShape(HEADER_PROGRESS_HEIGHT / 2)
    Box(
        Modifier
            .padding(vertical = 4.dp)
            .size(HEADER_PROGRESS_WIDTH, HEADER_PROGRESS_HEIGHT)
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(trackShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
