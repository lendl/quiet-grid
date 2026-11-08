package com.quietgrid.app.games.blockescape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val MINI_COLS = 6
private const val MINI_ROWS = 4
private val MINI_CELL = 10.dp

private class MiniBlock(val row: Int, val col: Int, val length: Int, val horizontal: Boolean, val key: Boolean = false, val ghost: Boolean = false)

@Composable
internal fun BlockEscapeSlideAlongPicture() {
    BlockEscapeMiniPicture(
        listOf(
            MiniBlock(1, 0, 2, true, ghost = true),
            MiniBlock(1, 2, 2, true),
            MiniBlock(0, 5, 3, false),
        ),
        exitRow = null,
    )
}

@Composable
internal fun BlockEscapeNoPassingPicture() {
    BlockEscapeMiniPicture(
        listOf(
            MiniBlock(1, 0, 2, true),
            MiniBlock(0, 3, 3, false),
            MiniBlock(3, 1, 2, true),
        ),
        exitRow = null,
    )
}

@Composable
internal fun BlockEscapeExitPicture() {
    BlockEscapeMiniPicture(
        listOf(
            MiniBlock(1, 0, 2, true, key = true, ghost = true),
            MiniBlock(1, 4, 2, true, key = true),
            MiniBlock(2, 2, 2, false),
        ),
        exitRow = 1,
    )
}

@Composable
private fun BlockEscapeMiniPicture(blocks: List<MiniBlock>, exitRow: Int?) {
    val colors = rememberBlockEscapeColors()
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridLine = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(MINI_CELL * (MINI_COLS + 0.4f), MINI_CELL * MINI_ROWS)) {
        val cell = MINI_CELL.toPx()
        drawRoundRect(background, size = Size(cell * MINI_COLS, cell * MINI_ROWS), cornerRadius = CornerRadius(cell * 0.3f))
        for (k in 1 until MINI_COLS) drawLine(gridLine, Offset(k * cell, 0f), Offset(k * cell, MINI_ROWS * cell), 1f)
        for (k in 1 until MINI_ROWS) drawLine(gridLine, Offset(0f, k * cell), Offset(MINI_COLS * cell, k * cell), 1f)
        if (exitRow != null) {
            drawRect(colors.key, topLeft = Offset(cell * (MINI_COLS + 0.1f), exitRow * cell + cell * 0.1f), size = Size(cell * 0.15f, cell * 0.8f))
        }
        val inset = cell * 0.1f
        blocks.forEach { block ->
            val color: Color = if (block.key) colors.key else colors.block
            val topLeft = Offset(block.col * cell + inset, block.row * cell + inset)
            val size = Size(
                (if (block.horizontal) block.length else 1) * cell - inset * 2,
                (if (block.horizontal) 1 else block.length) * cell - inset * 2,
            )
            if (block.ghost) {
                drawRoundRect(color.copy(alpha = 0.6f), topLeft, size, CornerRadius(cell * 0.25f), style = Stroke(cell * 0.08f))
            } else {
                drawRoundRect(color, topLeft, size, CornerRadius(cell * 0.25f))
            }
        }
    }
}
