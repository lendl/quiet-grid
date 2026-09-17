package com.quietgrid.app.games.nback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private const val NBACK_GRID_DIMENSION = 3
private val NBACK_CELL_GAP = 6.dp
private val NBACK_LIT_COLOR_PENCIL = Color(0xFF141414)
private val NBACK_CELL_COLOR_PENCIL = Color(0xFFE0E0E0)

@Composable
fun NBackGrid(litPosition: Int?) {
    val pencil = LocalIsPencilTheme.current
    val litColor = if (pencil) NBACK_LIT_COLOR_PENCIL else MaterialTheme.colorScheme.primary
    val cellColor = if (pencil) NBACK_CELL_COLOR_PENCIL else MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / NBACK_GRID_DIMENSION, maxHeight / NBACK_GRID_DIMENSION)
        Box(Modifier.size(cellSize * NBACK_GRID_DIMENSION)) {
            for (row in 0 until NBACK_GRID_DIMENSION) {
                for (col in 0 until NBACK_GRID_DIMENSION) {
                    val position = row * NBACK_GRID_DIMENSION + col
                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize - NBACK_CELL_GAP)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (position == litPosition) litColor else cellColor),
                    )
                }
            }
        }
    }
}
