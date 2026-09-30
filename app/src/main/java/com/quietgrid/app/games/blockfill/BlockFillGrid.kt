package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private enum class BlockFillHue { BLUE, CYAN, GREEN, ORANGE, PURPLE, RED }

private val FAMILY_HUES = mapOf(
    BlockFillShapeFamily.SINGLE to BlockFillHue.BLUE,
    BlockFillShapeFamily.DOMINO to BlockFillHue.BLUE,
    BlockFillShapeFamily.DIAGONAL_DOMINO to BlockFillHue.BLUE,
    BlockFillShapeFamily.STRAIGHT3 to BlockFillHue.CYAN,
    BlockFillShapeFamily.STRAIGHT4 to BlockFillHue.CYAN,
    BlockFillShapeFamily.STRAIGHT5 to BlockFillHue.CYAN,
    BlockFillShapeFamily.CORNER_TROMINO to BlockFillHue.GREEN,
    BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to BlockFillHue.GREEN,
    BlockFillShapeFamily.SQUARE2X2 to BlockFillHue.ORANGE,
    BlockFillShapeFamily.LJ to BlockFillHue.ORANGE,
    BlockFillShapeFamily.RECTANGLE to BlockFillHue.ORANGE,
    BlockFillShapeFamily.T_TETROMINO to BlockFillHue.PURPLE,
    BlockFillShapeFamily.PLUS to BlockFillHue.PURPLE,
    BlockFillShapeFamily.SZ to BlockFillHue.RED,
    BlockFillShapeFamily.SQUARE3X3 to BlockFillHue.RED,
)

private val DARK_HUE_COLORS = mapOf(
    BlockFillHue.BLUE to Color(0xFF8DB4E6),
    BlockFillHue.CYAN to Color(0xFF79C2BE),
    BlockFillHue.GREEN to Color(0xFF98C6A0),
    BlockFillHue.ORANGE to Color(0xFFE3B488),
    BlockFillHue.PURPLE to Color(0xFFBBA3EC),
    BlockFillHue.RED to Color(0xFFDE929C),
)

private val LIGHT_HUE_COLORS = mapOf(
    BlockFillHue.BLUE to Color(0xFF6E9FDB),
    BlockFillHue.CYAN to Color(0xFF5BB0AB),
    BlockFillHue.GREEN to Color(0xFF7DB587),
    BlockFillHue.ORANGE to Color(0xFFD9A066),
    BlockFillHue.PURPLE to Color(0xFFA68BE3),
    BlockFillHue.RED to Color(0xFFD57A86),
)

private val PENCIL_FAMILY_COLOR = Color(0xFF808080)
private val DARK_STONE_COLOR = Color(0xFF4A5260)
private val LIGHT_STONE_COLOR = Color(0xFFB4BBC5)
private val PENCIL_STONE_COLOR = Color(0xFFBDBDBD)

private const val TILE_CORNER_FRACTION = 0.14f
private const val GHOST_ALPHA = 0.45f
private const val DARK_BAND_ALPHA = 0.28f
private const val LIGHT_BAND_ALPHA = 0.16f
private const val PENCIL_BAND_ALPHA = 0.10f

private val WELL_CORNER_RADIUS = 10.dp
private val WELL_PADDING = 2.dp
private val BAND_CORNER_RADIUS = 6.dp
private val BAND_OVERHANG = 1.dp
val BLOCKFILL_BOARD_TILE_GAP = 1.5.dp

@Composable
fun blockFillIsDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun blockFillFamilyColor(family: BlockFillShapeFamily): Color {
    val isPencil = LocalIsPencilTheme.current
    val isDark = blockFillIsDarkSurface()
    if (family == BlockFillShapeFamily.STONE) {
        return when {
            isPencil -> PENCIL_STONE_COLOR
            isDark -> DARK_STONE_COLOR
            else -> LIGHT_STONE_COLOR
        }
    }
    return when {
        isPencil -> PENCIL_FAMILY_COLOR
        isDark -> DARK_HUE_COLORS.getValue(FAMILY_HUES.getValue(family))
        else -> LIGHT_HUE_COLORS.getValue(FAMILY_HUES.getValue(family))
    }
}

@Composable
fun BlockFillCell(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * TILE_CORNER_FRACTION))
            .background(color),
    )
}

data class BlockFillDragPreview(
    val pieceCells: List<Pair<Int, Int>>,
    val family: BlockFillShapeFamily,
    val anchor: BlockFillDragAnchor?,
    val clearedLines: BlockFillClearedLines,
)

@Composable
fun BlockFillGrid(
    board: BlockFillBoard,
    dragPreview: BlockFillDragPreview?,
    modifier: Modifier = Modifier,
    onBoardMeasured: (coordinates: LayoutCoordinates) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isPencil = LocalIsPencilTheme.current
    val isDark = blockFillIsDarkSurface()
    val wellColor = if (isDark) colorScheme.surfaceContainerLowest else colorScheme.surfaceContainerHighest
    val slotColor = colorScheme.surface
    val bandColor = colorScheme.primary.copy(
        alpha = when {
            isPencil -> PENCIL_BAND_ALPHA
            isDark -> DARK_BAND_ALPHA
            else -> LIGHT_BAND_ALPHA
        },
    )

    var previousBoard by remember { mutableStateOf(board) }
    var nextBatchId by remember { mutableStateOf(0L) }
    var clearingBatches by remember { mutableStateOf(listOf<BlockFillClearingBatch>()) }

    LaunchedEffect(board) {
        val prior = previousBoard
        previousBoard = board
        val cleared = mutableListOf<BlockFillClearingCell>()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                val priorFamily = prior[row][col]
                if (priorFamily != null && board[row][col] == null) {
                    cleared.add(BlockFillClearingCell(row, col, priorFamily))
                }
            }
        }
        if (cleared.isNotEmpty()) {
            clearingBatches = clearingBatches + BlockFillClearingBatch(nextBatchId++, cleared)
        }
    }

    Box(modifier) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val pitch = (min(maxWidth, maxHeight) - WELL_PADDING * 2) / BLOCKFILL_BOARD_SIZE
            val boardSize = pitch * BLOCKFILL_BOARD_SIZE
            val tileSize = pitch - BLOCKFILL_BOARD_TILE_GAP
            val tileInset = BLOCKFILL_BOARD_TILE_GAP / 2
            val ghostAnchor = dragPreview?.anchor
            val ghostCells = if (ghostAnchor != null) {
                dragPreview.pieceCells.map { (dr, dc) -> (ghostAnchor.row + dr) to (ghostAnchor.col + dc) }.toSet()
            } else {
                emptySet()
            }
            val clearedLines = dragPreview?.clearedLines
            val clearRows = clearedLines?.rows.orEmpty()
            val clearCols = clearedLines?.cols.orEmpty()

            Box(
                Modifier
                    .clip(RoundedCornerShape(WELL_CORNER_RADIUS))
                    .background(wellColor)
                    .padding(WELL_PADDING),
            ) {
                Box(
                    Modifier
                        .size(boardSize)
                        .onGloballyPositioned { coordinates -> onBoardMeasured(coordinates) },
                ) {
                    val bandShape = RoundedCornerShape(BAND_CORNER_RADIUS)
                    for (row in clearRows) {
                        Box(
                            Modifier
                                .offset(y = pitch * row - BAND_OVERHANG)
                                .size(boardSize, pitch + BAND_OVERHANG * 2)
                                .clip(bandShape)
                                .background(bandColor),
                        )
                    }
                    for (col in clearCols) {
                        Box(
                            Modifier
                                .offset(x = pitch * col - BAND_OVERHANG)
                                .size(pitch + BAND_OVERHANG * 2, boardSize)
                                .clip(bandShape)
                                .background(bandColor),
                        )
                    }

                    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
                        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                            val cellKey = row to col
                            val filledFamily = board[row][col]
                            val tileOffset = Modifier.offset(x = pitch * col + tileInset, y = pitch * row + tileInset)
                            when {
                                filledFamily != null -> BlockFillCell(blockFillFamilyColor(filledFamily), tileSize, tileOffset)
                                cellKey in ghostCells -> BlockFillCell(
                                    blockFillFamilyColor(dragPreview!!.family).copy(alpha = GHOST_ALPHA),
                                    tileSize,
                                    tileOffset,
                                )
                                row in clearRows || col in clearCols -> Unit
                                else -> BlockFillCell(slotColor, tileSize, tileOffset)
                            }
                        }
                    }

                    for (batch in clearingBatches) {
                        key(batch.id) {
                            BlockFillClearingOverlay(
                                cells = batch.cells,
                                pitch = pitch,
                                tileSize = tileSize,
                                tileInset = tileInset,
                                onFinished = { clearingBatches = clearingBatches.filterNot { it.id == batch.id } },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BlockFillClearingCell(val row: Int, val col: Int, val family: BlockFillShapeFamily)
private data class BlockFillClearingBatch(val id: Long, val cells: List<BlockFillClearingCell>)

@Composable
private fun BlockFillClearingOverlay(
    cells: List<BlockFillClearingCell>,
    pitch: Dp,
    tileSize: Dp,
    tileInset: Dp,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        onFinished()
    }
    for (cell in cells) {
        BlockFillCell(
            color = blockFillFamilyColor(cell.family),
            size = tileSize,
            modifier = Modifier
                .offset(x = pitch * cell.col + tileInset, y = pitch * cell.row + tileInset)
                .graphicsLayer(
                    alpha = 1f - progress.value,
                    scaleX = 1f - progress.value * 0.4f,
                    scaleY = 1f - progress.value * 0.4f,
                ),
        )
    }
}
