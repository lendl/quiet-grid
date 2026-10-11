package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.floor

private enum class BlockFillHue { BLUE, CYAN, GREEN, ORANGE, PURPLE, RED, YELLOW }

private val FAMILY_HUES = mapOf(
    BlockFillShapeFamily.SINGLE to BlockFillHue.GREEN,
    BlockFillShapeFamily.DOMINO to BlockFillHue.GREEN,
    BlockFillShapeFamily.DIAGONAL_DOMINO to BlockFillHue.GREEN,
    BlockFillShapeFamily.STRAIGHT3 to BlockFillHue.ORANGE,
    BlockFillShapeFamily.STRAIGHT4 to BlockFillHue.ORANGE,
    BlockFillShapeFamily.STRAIGHT5 to BlockFillHue.ORANGE,
    BlockFillShapeFamily.CORNER_TROMINO to BlockFillHue.CYAN,
    BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to BlockFillHue.CYAN,
    BlockFillShapeFamily.SQUARE2X2 to BlockFillHue.PURPLE,
    BlockFillShapeFamily.RECTANGLE to BlockFillHue.PURPLE,
    BlockFillShapeFamily.LJ to BlockFillHue.YELLOW,
    BlockFillShapeFamily.T_TETROMINO to BlockFillHue.RED,
    BlockFillShapeFamily.PLUS to BlockFillHue.RED,
    BlockFillShapeFamily.SZ to BlockFillHue.BLUE,
    BlockFillShapeFamily.SQUARE3X3 to BlockFillHue.BLUE,
)

private val HUE_COLORS = mapOf(
    BlockFillHue.BLUE to Color(0xFF2D5CF0),
    BlockFillHue.CYAN to Color(0xFF22C8F0),
    BlockFillHue.GREEN to Color(0xFF17C23A),
    BlockFillHue.ORANGE to Color(0xFFF59A23),
    BlockFillHue.PURPLE to Color(0xFFA44BF0),
    BlockFillHue.RED to Color(0xFFE0343C),
    BlockFillHue.YELLOW to Color(0xFFF5BE0B),
)

private val PENCIL_FAMILY_COLOR = Color(0xFF808080)
private val DARK_STONE_COLOR = Color(0xFF4A5260)
private val LIGHT_STONE_COLOR = Color(0xFFB4BBC5)
private val PENCIL_STONE_COLOR = Color(0xFFBDBDBD)

private const val TILE_CORNER_FRACTION = 0.14f
private const val BEVEL_FRACTION = 0.15f
private const val TILE_RADIUS_FRACTION = 0.06f
private const val BEVEL_TOP_LIGHTEN = 0.48f
private const val BEVEL_LEFT_LIGHTEN = 0.26f
private const val BEVEL_RIGHT_DARKEN = 0.20f
private const val BEVEL_BOTTOM_DARKEN = 0.36f
private const val TILE_OUTLINE_ALPHA = 0.35f
private const val GHOST_ALPHA = 0.45f
private const val GHOST_OUTLINE_ALPHA = 0.8f
private const val SLOT_OUTLINE_ALPHA = 0.6f
private const val DARK_BAND_ALPHA = 0.28f
private const val LIGHT_BAND_ALPHA = 0.16f
private const val PENCIL_BAND_ALPHA = 0.10f

private val WELL_CORNER_RADIUS = 10.dp
private val WELL_PADDING = 2.dp
private val WELL_RIM_WIDTH = 2.dp
private val SLOT_OUTLINE_WIDTH = 1.dp
private val GHOST_OUTLINE_WIDTH = 2.dp
private val BAND_CORNER_RADIUS = 6.dp
private val BAND_OVERHANG = 1.dp
val BLOCKFILL_BOARD_TILE_GAP = 0.dp

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
    return if (isPencil) PENCIL_FAMILY_COLOR else HUE_COLORS.getValue(FAMILY_HUES.getValue(family))
}

@Composable
fun BlockFillCell(color: Color, size: Dp, modifier: Modifier = Modifier, border: BorderStroke? = null) {
    val shape = RoundedCornerShape(size * TILE_CORNER_FRACTION)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
    )
}

@Composable
fun BlockFillTile(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * TILE_RADIUS_FRACTION))
            .drawBehind { drawBevelledTile(color) },
    )
}

private fun DrawScope.drawBevelledTile(color: Color) {
    val w = size.width
    val h = size.height
    val b = minOf(w, h) * BEVEL_FRACTION
    drawRect(color)
    val top = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w - b, b); lineTo(b, b); close() }
    val left = Path().apply { moveTo(0f, 0f); lineTo(b, b); lineTo(b, h - b); lineTo(0f, h); close() }
    val right = Path().apply { moveTo(w, 0f); lineTo(w, h); lineTo(w - b, h - b); lineTo(w - b, b); close() }
    val bottom = Path().apply { moveTo(0f, h); lineTo(b, h - b); lineTo(w - b, h - b); lineTo(w, h); close() }
    drawPath(top, lerp(color, Color.White, BEVEL_TOP_LIGHTEN))
    drawPath(left, lerp(color, Color.White, BEVEL_LEFT_LIGHTEN))
    drawPath(right, lerp(color, Color.Black, BEVEL_RIGHT_DARKEN))
    drawPath(bottom, lerp(color, Color.Black, BEVEL_BOTTOM_DARKEN))
    drawRect(Color.Black.copy(alpha = TILE_OUTLINE_ALPHA), style = Stroke(width = 1f))
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
    val slotBorder = BorderStroke(SLOT_OUTLINE_WIDTH, colorScheme.outline.copy(alpha = SLOT_OUTLINE_ALPHA))
    val ghostBorder = BorderStroke(GHOST_OUTLINE_WIDTH, colorScheme.onSurface.copy(alpha = GHOST_OUTLINE_ALPHA))
    val wellShape = RoundedCornerShape(WELL_CORNER_RADIUS)
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
            val pitch = with(LocalDensity.current) {
                floor((min(maxWidth, maxHeight) - WELL_PADDING * 2).toPx() / BLOCKFILL_BOARD_SIZE).toDp()
            }
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
                    .clip(wellShape)
                    .background(wellColor)
                    .border(WELL_RIM_WIDTH, colorScheme.outline, wellShape)
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
                                filledFamily != null -> BlockFillTile(blockFillFamilyColor(filledFamily), tileSize, tileOffset)
                                cellKey in ghostCells -> BlockFillCell(
                                    blockFillFamilyColor(dragPreview!!.family).copy(alpha = GHOST_ALPHA),
                                    tileSize,
                                    tileOffset,
                                    border = ghostBorder,
                                )
                                row in clearRows || col in clearCols -> Unit
                                else -> BlockFillCell(slotColor, tileSize, tileOffset, border = slotBorder)
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
        BlockFillTile(
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
