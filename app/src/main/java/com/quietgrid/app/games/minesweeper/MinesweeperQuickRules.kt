package com.quietgrid.app.games.minesweeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val MINI_CELL_SIZE = 16.dp
private val MINI_CELL_SHAPE = RoundedCornerShape(percent = 20)

private enum class MiniCell { HIDDEN, OPEN, ONE, FLAG }

@Composable
internal fun MinesweeperRevealPicture() = MinesweeperMiniRow(listOf(MiniCell.OPEN, MiniCell.ONE, MiniCell.HIDDEN, MiniCell.HIDDEN))

@Composable
internal fun MinesweeperNumbersPicture() = MinesweeperMiniRow(listOf(MiniCell.ONE, MiniCell.FLAG, MiniCell.ONE))

@Composable
internal fun MinesweeperFlagPicture() = MinesweeperMiniRow(listOf(MiniCell.HIDDEN, MiniCell.FLAG, MiniCell.HIDDEN))

@Composable
private fun MinesweeperMiniRow(cells: List<MiniCell>) {
    val isPencilTheme = LocalIsPencilTheme.current
    val palette = minesweeperPaletteFor(MaterialTheme.colorScheme.surface)
    val hidden = if (isPencilTheme) MaterialTheme.colorScheme.surfaceVariant else palette.hidden
    val opened = if (isPencilTheme) MaterialTheme.colorScheme.surface else palette.opened
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        cells.forEach { cell ->
            val revealed = cell == MiniCell.OPEN || cell == MiniCell.ONE
            Box(
                Modifier.size(MINI_CELL_SIZE).background(if (revealed) opened else hidden, MINI_CELL_SHAPE),
                contentAlignment = Alignment.Center,
            ) {
                when (cell) {
                    MiniCell.ONE -> Text(
                        "1",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold),
                        color = palette.numbers.first(),
                    )
                    MiniCell.FLAG -> MinesweeperFlagGlyph(
                        poleColor = MaterialTheme.colorScheme.onSurface,
                        bannerColor = MaterialTheme.colorScheme.primary,
                    )
                    else -> Unit
                }
            }
        }
    }
}
