package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.play.DockRole
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.isUpright
import kotlin.math.roundToInt

private val FLOATING_PIECE_LIFT = 72.dp
private const val FLOATING_PIECE_ALPHA = 0.9f
private val FLOATING_PIECE_SHADOW_OFFSET = 4.dp
private val STRIP_PROGRESS_WIDTH = 72.dp
private val STRIP_PROGRESS_HEIGHT = 4.dp
private val TRAY_TOP_PADDING = 8.dp
private val TRAY_INNER_PADDING = 16.dp

private fun blockFillTrayCellSize(sizeClass: PlaySizeClass): Dp = when (sizeClass) {
    PlaySizeClass.Compact -> 20.dp
    PlaySizeClass.Medium -> 23.dp
    PlaySizeClass.PhoneLandscape -> 18.dp
    PlaySizeClass.TabletLandscape, PlaySizeClass.TabletPortrait -> 32.dp
}

private fun blockFillDockHeight(sizeClass: PlaySizeClass): Dp =
    TRAY_TOP_PADDING + TRAY_INNER_PADDING + blockFillTrayCellSize(sizeClass) * 5

@Composable
fun BlockFillPlayLayout(
    gameId: GameId,
    board: BlockFillBoard?,
    tray: List<BlockFillPiece?>?,
    playFresh: Boolean,
    onPlacePiece: (pieceIndex: Int, anchorRow: Int, anchorCol: Int) -> Boolean,
    timer: PlayTimer?,
    onBack: () -> Unit,
    onEndPuzzle: () -> Unit,
    info: List<InfoItem>,
    difficulty: Difficulty? = null,
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
        GamePlayScaffold(
            gameId = gameId,
            timer = timer,
            onBack = onBack,
            onEndPuzzle = onEndPuzzle,
            difficulty = difficulty,
            info = info,
            dockRole = DockRole.DragSource,
            dockHeight = ::blockFillDockHeight,
            dock = { sizeClass ->
                if (tray != null) {
                    val vertical = !sizeClass.isUpright
                    BlockFillTray(
                        tray = tray,
                        placeablePieces = placeablePieces,
                        draggingPieceIndex = draggingPieceIndex,
                        cellSize = blockFillTrayCellSize(sizeClass),
                        vertical = vertical,
                        modifier = if (vertical) Modifier.fillMaxSize() else Modifier.padding(top = TRAY_TOP_PADDING),
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
            },
            board = {
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
            },
        )

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
                shadowOffset = FLOATING_PIECE_SHADOW_OFFSET,
                alpha = floatingAlpha,
                modifier = Modifier
                    .offset {
                        val topLeft = floatingPieceTopLeft(dragPointer.x, dragPointer.y, floatingPiece.cells, floatingCellSizePx, liftPx)
                        IntOffset(
                            (topLeft.x - screenOrigin.x).roundToInt(),
                            (topLeft.y - screenOrigin.y).roundToInt(),
                        )
                    }
                    .graphicsLayer { translationX = rejectedDropShakeX.value },
            )
        }
    }
}

@Composable
internal fun BlockFillScoreText(score: Int) {
    val scoreLabel = stringResource(R.string.blockfill_score_label)
    Text(
        score.toString(),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { contentDescription = "$scoreLabel $score" },
    )
}

@Composable
internal fun BlockFillMultiplierText(multiplier: Int) {
    Text(
        stringResource(R.string.blockfill_multiplier_value, multiplier),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun BlockFillStripProgress(progress: Float) {
    val trackShape = RoundedCornerShape(STRIP_PROGRESS_HEIGHT / 2)
    Box(
        Modifier
            .size(STRIP_PROGRESS_WIDTH, STRIP_PROGRESS_HEIGHT)
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

@Composable
internal fun BlockFillStripRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}

@Composable
internal fun BlockFillLinePicture(rows: Int, family: BlockFillShapeFamily, trailing: String? = null) {
    val color = blockFillFamilyColor(family)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            repeat(rows) {
                Row { repeat(5) { BlockFillTile(color = color, size = 10.dp) } }
            }
        }
        if (trailing != null) {
            Text(trailing, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun BlockFillPlanAheadPicture() {
    BlockFillPieceGlyph(
        piece = BlockFillPiece(shapeId = "quick-rule", family = BlockFillShapeFamily.LJ, cells = listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)),
        cellSize = 10.dp,
    )
}
