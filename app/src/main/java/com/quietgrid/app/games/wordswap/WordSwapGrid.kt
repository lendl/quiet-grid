package com.quietgrid.app.games.wordswap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.components.LocalReduceMotion
import com.quietgrid.engine.wordswap.WORDSWAP_HOLE
import com.quietgrid.engine.wordswap.WordSwapDirection
import com.quietgrid.engine.wordswap.WordSwapMove
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry

private const val TILE_GAP = 0.06f
private const val TILE_CORNER = 0.18f
private const val LETTER_SIZE = 0.5f
private const val SWAP_MILLIS = 220

@Composable
fun WordSwapGrid(
    puzzle: WordSwapPuzzleEntry,
    board: String,
    selectedCell: Int?,
    doneCells: Set<Int>,
    hintCells: Set<Int>,
    lastSwap: WordSwapMove?,
    swapSerial: Int,
    onTileTap: (Int) -> Unit,
) {
    val colors = rememberWordSwapColors()
    val reduceMotion = LocalReduceMotion.current
    val swapProgress = remember { Animatable(1f) }
    LaunchedEffect(swapSerial) {
        if (lastSwap != null && !reduceMotion) {
            swapProgress.snapTo(0f)
            swapProgress.animateTo(1f, tween(SWAP_MILLIS))
        } else {
            swapProgress.snapTo(1f)
        }
    }
    val diagonals = puzzle.slots.filter { it.direction == WordSwapDirection.DIAGONAL || it.direction == WordSwapDirection.ANTI_DIAGONAL }

    BoxWithConstraints(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / puzzle.cols, maxHeight / puzzle.rows)
        val density = LocalDensity.current
        val cellPx = with(density) { cellSize.toPx() }
        val letterSize = with(density) { (cellSize * LETTER_SIZE).toSp() }
        Box(
            Modifier
                .size(cellSize * puzzle.cols, cellSize * puzzle.rows)
                .drawBehind {
                    diagonals.forEach { slot ->
                        val last = slot.length - 1
                        val start = Offset((slot.col + 0.5f) * cellPx, (slot.row + 0.5f) * cellPx)
                        val end = Offset(
                            (slot.col + last * slot.direction.colStep + 0.5f) * cellPx,
                            (slot.row + last * slot.direction.rowStep + 0.5f) * cellPx,
                        )
                        drawLine(colors.diagonal, start, end, strokeWidth = cellPx * 0.08f, cap = StrokeCap.Round)
                    }
                },
        ) {
            board.indices.filter { board[it] != WORDSWAP_HOLE }.forEach { cell ->
                key(cell) {
                    val letter = board[cell]
                    val row = cell / puzzle.cols
                    val col = cell % puzzle.cols
                    val partner = when (cell) {
                        lastSwap?.first -> lastSwap.second
                        lastSwap?.second -> lastSwap.first
                        else -> null
                    }
                    val done = cell in doneCells
                    val hinted = cell in hintCells
                    val selected = cell == selectedCell
                    val fill by animateColorAsState(
                        when {
                            selected -> colors.selected
                            hinted -> colors.hint
                            done -> colors.done
                            else -> colors.tile
                        },
                        label = "wordswap-tile",
                    )
                    val textColor = when {
                        selected -> colors.onSelected
                        hinted -> colors.onHint
                        done -> colors.onDone
                        else -> colors.onTile
                    }
                    val shape = RoundedCornerShape(cellSize * TILE_CORNER)
                    Box(
                        Modifier
                            .offset(cellSize * col, cellSize * row)
                            .size(cellSize)
                            .zIndex(if (partner != null) 1f else 0f)
                            .graphicsLayer {
                                if (partner != null) {
                                    val remaining = 1f - swapProgress.value
                                    translationX = (partner % puzzle.cols - col) * cellPx * remaining
                                    translationY = (partner / puzzle.cols - row) * cellPx * remaining
                                }
                            }
                            .padding(cellSize * TILE_GAP)
                            .clip(shape)
                            .background(fill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onTileTap(cell) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            letter.uppercaseChar().toString(),
                            color = textColor,
                            fontSize = letterSize,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
