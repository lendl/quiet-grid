package com.quietgrid.app.games.blockescape

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.engine.blockescape.BLOCKESCAPE_EXIT_ROW
import com.quietgrid.engine.blockescape.BLOCKESCAPE_KEY_BLOCK
import com.quietgrid.engine.blockescape.BLOCKESCAPE_SIZE
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import com.quietgrid.engine.blockescape.blockEscapeBlockAt
import com.quietgrid.engine.blockescape.blockEscapeSlideRange
import com.quietgrid.engine.blockescape.movedTo
import com.quietgrid.engine.blockescape.position
import kotlin.math.floor
import kotlin.math.roundToInt

private const val BLOCKESCAPE_MARGIN = 0.22f
private const val BLOCK_INSET = 0.07f
private const val BLOCK_CORNER = 0.22f
private const val EXIT_WIDTH = 0.1f
private const val HINT_TARGET_ALPHA = 0.35f

@Composable
fun BlockEscapeGrid(
    blocks: List<BlockEscapeBlock>,
    hint: BlockEscapeMove?,
    onMove: (BlockEscapeMove) -> Unit,
    onPickUp: () -> Unit = {},
) {
    val colors = rememberBlockEscapeColors()
    val currentBlocks by rememberUpdatedState(blocks)
    val currentHint by rememberUpdatedState(hint)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnPickUp by rememberUpdatedState(onPickUp)
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridLine = MaterialTheme.colorScheme.outlineVariant
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val span = BLOCKESCAPE_SIZE + BLOCKESCAPE_MARGIN * 2
        val cellSize = min(maxWidth, maxHeight) / span
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }
        val marginPx = cellPx * BLOCKESCAPE_MARGIN

        Box(
            Modifier
                .size(cellSize * span, cellSize * span)
                .pointerInput(cellPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val col = floor((down.position.x - marginPx) / cellPx).toInt()
                        val row = floor((down.position.y - marginPx) / cellPx).toInt()
                        val snapshot = currentBlocks
                        val index = blockEscapeBlockAt(snapshot, row, col) ?: return@awaitEachGesture
                        down.consume()
                        val block = snapshot[index]
                        val range = blockEscapeSlideRange(snapshot, index)
                        val minOffset = (range.first - block.position) * cellPx
                        val maxOffset = (range.last - block.position) * cellPx
                        dragIndex = index
                        dragOffset = 0f
                        currentOnPickUp()
                        val start = down.position
                        val completed = drag(down.id) { change ->
                            change.consume()
                            val delta = change.position - start
                            dragOffset = (if (block.horizontal) delta.x else delta.y).coerceIn(minOffset, maxOffset)
                        }
                        val steps = (dragOffset / cellPx).roundToInt()
                        dragIndex = null
                        dragOffset = 0f
                        if (completed && steps != 0) currentOnMove(BlockEscapeMove(index, block.position + steps))
                    }
                }
                .drawBehind {
                    val gridPx = cellPx * BLOCKESCAPE_SIZE
                    drawRoundRect(
                        background,
                        topLeft = Offset(marginPx, marginPx),
                        size = Size(gridPx, gridPx),
                        cornerRadius = CornerRadius(cellPx * 0.25f),
                    )
                    val lineWidth = maxOf(1f, cellPx * 0.02f)
                    for (k in 1 until BLOCKESCAPE_SIZE) {
                        drawLine(gridLine, Offset(marginPx + k * cellPx, marginPx), Offset(marginPx + k * cellPx, marginPx + gridPx), lineWidth)
                        drawLine(gridLine, Offset(marginPx, marginPx + k * cellPx), Offset(marginPx + gridPx, marginPx + k * cellPx), lineWidth)
                    }
                    val exitTop = marginPx + BLOCKESCAPE_EXIT_ROW * cellPx + cellPx * BLOCK_INSET
                    drawRoundRect(
                        colors.key,
                        topLeft = Offset(marginPx + gridPx + cellPx * 0.05f, exitTop),
                        size = Size(cellPx * EXIT_WIDTH, cellPx * (1f - BLOCK_INSET * 2)),
                        cornerRadius = CornerRadius(cellPx * 0.05f),
                    )
                    val inset = cellPx * BLOCK_INSET
                    val shownHint = currentHint?.takeIf { it.block != dragIndex && it.block in currentBlocks.indices }
                    fun blockSize(block: BlockEscapeBlock) = Size(
                        (if (block.horizontal) block.length else 1) * cellPx - inset * 2,
                        (if (block.horizontal) 1 else block.length) * cellPx - inset * 2,
                    )
                    fun hintFill(index: Int) = if (index == BLOCKESCAPE_KEY_BLOCK) colors.key else colors.hint
                    if (shownHint != null) {
                        val target = currentBlocks[shownHint.block].movedTo(shownHint.to)
                        drawRoundRect(
                            hintFill(shownHint.block).copy(alpha = HINT_TARGET_ALPHA),
                            topLeft = Offset(marginPx + target.col * cellPx + inset, marginPx + target.row * cellPx + inset),
                            size = blockSize(target),
                            cornerRadius = CornerRadius(cellPx * BLOCK_CORNER),
                        )
                    }
                    currentBlocks.forEachIndexed { index, block ->
                        val shift = if (index == dragIndex) dragOffset else 0f
                        val left = marginPx + block.col * cellPx + inset + if (block.horizontal) shift else 0f
                        val top = marginPx + block.row * cellPx + inset + if (block.horizontal) 0f else shift
                        val fill = when {
                            index == BLOCKESCAPE_KEY_BLOCK -> colors.key
                            index == dragIndex -> colors.activeBlock
                            index == shownHint?.block -> hintFill(index)
                            else -> colors.block
                        }
                        drawRoundRect(fill, topLeft = Offset(left, top), size = blockSize(block), cornerRadius = CornerRadius(cellPx * BLOCK_CORNER))
                    }
                },
        )
    }
}
