package com.quietgrid.app.games.game2048

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.abs
import kotlin.math.max

private val GAP = 4.dp
private val SWIPE_THRESHOLD_DP = 24.dp

private val TILE_COLORS = mapOf(
    2 to Color(0xFFEEE4DA),
    4 to Color(0xFFEDE0C8),
    8 to Color(0xFFF2B179),
    16 to Color(0xFFF59563),
    32 to Color(0xFFF67C5F),
    64 to Color(0xFFF65E3B),
    128 to Color(0xFFEDCF72),
    256 to Color(0xFFEDCC61),
    512 to Color(0xFFEDC850),
    1024 to Color(0xFFEDC53F),
    2048 to Color(0xFFEDC22E),
)

private val PENCIL_TILE_COLORS = mapOf(
    2 to Color(0xFFE0E0E0),
    4 to Color(0xFFCCCCCC),
    8 to Color(0xFFB8B8B8),
    16 to Color(0xFFA3A3A3),
    32 to Color(0xFF8F8F8F),
    64 to Color(0xFF7A7A7A),
    128 to Color(0xFF666666),
    256 to Color(0xFF525252),
    512 to Color(0xFF3D3D3D),
    1024 to Color(0xFF292929),
    2048 to Color(0xFF141414),
)

private val EMPTY_CELL_COLOR = Color(0xFFCDC1B4)
private val EMPTY_CELL_COLOR_PENCIL = Color(0xFFD8D8D8)

private fun tileColor(value: Int, pencil: Boolean): Color =
    (if (pencil) PENCIL_TILE_COLORS else TILE_COLORS)[value] ?: (if (pencil) Color(0xFF000000) else Color(0xFF3C3A32))

private fun tileTextColor(value: Int, pencil: Boolean): Color = when {
    !pencil && value <= 4 -> Color(0xFF776E65)
    pencil && value <= 8 -> Color.Black
    else -> Color.White
}

@Composable
fun Game2048Grid(board: Game2048Board, onSwipe: (Game2048Direction) -> Unit) {
    val pencil = LocalIsPencilTheme.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.toPx() }

    Box(
        Modifier.pointerInput(Unit) {
            var totalDrag = Offset.Zero
            detectDragGestures(
                onDragStart = { totalDrag = Offset.Zero },
                onDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    val dx = totalDrag.x
                    val dy = totalDrag.y
                    if (max(abs(dx), abs(dy)) > thresholdPx) {
                        val direction = if (abs(dx) > abs(dy)) {
                            if (dx > 0) Game2048Direction.RIGHT else Game2048Direction.LEFT
                        } else {
                            if (dy > 0) Game2048Direction.DOWN else Game2048Direction.UP
                        }
                        onSwipe(direction)
                    }
                },
            )
        },
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val cellSize = min(maxWidth / board.size, maxHeight / board.size)
            val fontSize = (cellSize.value * 0.38f).sp

            Box(Modifier.size(cellSize * board.size)) {
                for (row in 0 until board.size) {
                    for (col in 0 until board.size) {
                        val value = board.tiles[row][col]
                        Box(
                            modifier = Modifier
                                .offset(x = cellSize * col, y = cellSize * row)
                                .size(cellSize - GAP)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (value != null) tileColor(value, pencil) else (if (pencil) EMPTY_CELL_COLOR_PENCIL else EMPTY_CELL_COLOR)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != null) {
                                BasicText(
                                    text = value.toString(),
                                    style = TextStyle(
                                        fontSize = fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = tileTextColor(value, pencil),
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
