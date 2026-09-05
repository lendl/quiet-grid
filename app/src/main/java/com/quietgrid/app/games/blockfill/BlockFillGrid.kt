package com.quietgrid.app.games.blockfill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val FAMILY_COLORS = mapOf(
    BlockFillShapeFamily.SINGLE to Color(0xFF64B5F6),
    BlockFillShapeFamily.DOMINO to Color(0xFF64B5F6),
    BlockFillShapeFamily.DIAGONAL_DOMINO to Color(0xFF64B5F6),
    BlockFillShapeFamily.STRAIGHT3 to Color(0xFF4DD0E1),
    BlockFillShapeFamily.CORNER_TROMINO to Color(0xFF81C784),
    BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to Color(0xFF81C784),
    BlockFillShapeFamily.SQUARE2X2 to Color(0xFFFFB74D),
    BlockFillShapeFamily.STRAIGHT4 to Color(0xFF4DD0E1),
    BlockFillShapeFamily.T_TETROMINO to Color(0xFFBA68C8),
    BlockFillShapeFamily.SZ to Color(0xFFE57373),
    BlockFillShapeFamily.LJ to Color(0xFFFFB74D),
    BlockFillShapeFamily.PLUS to Color(0xFFBA68C8),
    BlockFillShapeFamily.STRAIGHT5 to Color(0xFF4DD0E1),
    BlockFillShapeFamily.RECTANGLE to Color(0xFFFFB74D),
    BlockFillShapeFamily.SQUARE3X3 to Color(0xFFE57373),
)

private val PENCIL_FAMILY_COLOR = Color(0xFF808080)

@Composable
fun blockFillFamilyColor(family: BlockFillShapeFamily): Color =
    if (LocalIsPencilTheme.current) PENCIL_FAMILY_COLOR else FAMILY_COLORS.getValue(family)

private const val BLOCK_CORNER_FRACTION = 0.08f
private const val BLOCK_BEVEL_FRACTION = 0.14f
private val BLOCK_BORDER_WIDTH = 1.5.dp
private val BLOCK_ELEVATION = 4.dp

@Composable
fun BlockFillCell(color: Color, size: Dp, modifier: Modifier = Modifier, borderColor: Color? = null, elevated: Boolean = false) {
    val shape = RoundedCornerShape(size * BLOCK_CORNER_FRACTION)
    val shadowTone = lerp(color, Color.Black, 0.45f)
    val highlightTone = lerp(color, Color.White, 0.35f)
    val outline = borderColor ?: lerp(color, Color.Black, 0.35f)
    val bevel = size * BLOCK_BEVEL_FRACTION

    Box(
        modifier
            .then(if (elevated) Modifier.shadow(BLOCK_ELEVATION, shape, clip = false) else Modifier)
            .size(size)
            .clip(shape)
            .border(BLOCK_BORDER_WIDTH, outline, shape),
    ) {
        Box(Modifier.size(size).background(shadowTone))
        Box(Modifier.size(size - bevel).background(highlightTone))
        Box(Modifier.offset(x = bevel, y = bevel).size(size - bevel * 2).background(color))
    }
}

data class BlockFillDragPreview(
    val pieceCells: List<Pair<Int, Int>>,
    val family: BlockFillShapeFamily,
    val anchor: BlockFillDragAnchor?,
    val clearedCells: Set<Pair<Int, Int>>,
)

private val BOARD_FRAME_PADDING = 10.dp
private val BOARD_FRAME_CORNER_RADIUS = 6.dp
private val BOARD_FRAME_BORDER_WIDTH = 1.dp
private val GRID_LINE_WIDTH = 1.dp

private fun Modifier.blockFillGridLines(cellSizePx: Float, color: Color): Modifier = drawWithContent {
    drawContent()
    val strokeWidth = GRID_LINE_WIDTH.toPx()
    for (i in 0..BLOCKFILL_BOARD_SIZE) {
        val x = cellSizePx * i
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
        val y = cellSizePx * i
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
    }
}

@Composable
fun BlockFillGrid(
    board: BlockFillBoard,
    dragPreview: BlockFillDragPreview?,
    modifier: Modifier = Modifier,
    onBoardMeasured: (coordinates: LayoutCoordinates) -> Unit = {},
) {
    val amberHighlight = Color(0xFFFFC107).copy(alpha = 0.55f)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val frameColor = MaterialTheme.colorScheme.surfaceVariant
    val frameBorderColor = MaterialTheme.colorScheme.outlineVariant

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
            val cellSize = (min(maxWidth, maxHeight) - BOARD_FRAME_PADDING * 2) / BLOCKFILL_BOARD_SIZE
            val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }
            val ghostAnchor = dragPreview?.anchor
            val ghostCells = if (ghostAnchor != null) {
                dragPreview.pieceCells.map { (dr, dc) -> (ghostAnchor.row + dr) to (ghostAnchor.col + dc) }.toSet()
            } else {
                emptySet()
            }

            Box(
                Modifier
                    .size(cellSize * BLOCKFILL_BOARD_SIZE + BOARD_FRAME_PADDING * 2)
                    .clip(RoundedCornerShape(BOARD_FRAME_CORNER_RADIUS))
                    .background(frameColor)
                    .border(BOARD_FRAME_BORDER_WIDTH, frameBorderColor, RoundedCornerShape(BOARD_FRAME_CORNER_RADIUS))
                    .padding(BOARD_FRAME_PADDING),
            ) {
                Box(
                    Modifier
                        .size(cellSize * BLOCKFILL_BOARD_SIZE)
                        .onGloballyPositioned { coordinates -> onBoardMeasured(coordinates) }
                        .blockFillGridLines(cellSizePx, gridLineColor),
                ) {
                    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
                        for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                            val cellKey = row to col
                            val filledFamily = board[row][col]
                            val isGhost = cellKey in ghostCells
                            val willClear = cellKey in dragPreview?.clearedCells.orEmpty()
                            val cellOffset = Modifier.offset(x = cellSize * col, y = cellSize * row)
                            when {
                                willClear -> Box(cellOffset.size(cellSize).background(amberHighlight))
                                isGhost -> BlockFillCell(
                                    color = blockFillFamilyColor(dragPreview!!.family).copy(alpha = 0.6f),
                                    size = cellSize,
                                    modifier = cellOffset,
                                    borderColor = blockFillFamilyColor(dragPreview.family),
                                )
                                filledFamily != null -> BlockFillCell(
                                    color = blockFillFamilyColor(filledFamily),
                                    size = cellSize,
                                    modifier = cellOffset,
                                )
                                else -> Box(cellOffset.size(cellSize).background(frameColor))
                            }
                        }
                    }

                    for (batch in clearingBatches) {
                        key(batch.id) {
                            BlockFillClearingOverlay(
                                cells = batch.cells,
                                cellSize = cellSize,
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
private fun BlockFillClearingOverlay(cells: List<BlockFillClearingCell>, cellSize: Dp, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        onFinished()
    }
    for (cell in cells) {
        BlockFillCell(
            color = blockFillFamilyColor(cell.family),
            size = cellSize,
            modifier = Modifier
                .offset(x = cellSize * cell.col, y = cellSize * cell.row)
                .graphicsLayer(
                    alpha = 1f - progress.value,
                    scaleX = 1f - progress.value * 0.4f,
                    scaleY = 1f - progress.value * 0.4f,
                ),
        )
    }
}
