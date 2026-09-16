package com.quietgrid.app.games.game2048

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val GAP = 4.dp
private const val SLIDE_DURATION_MS = 130
private const val POP_DURATION_MS = 90
private const val POP_SCALE = 1.15f

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
fun Game2048Grid(board: Game2048Board, lastMove: Game2048MoveResult? = null) {
    val pencil = LocalIsPencilTheme.current
    var displayBoard by remember { mutableStateOf(board) }
    var slidingMoves by remember { mutableStateOf<List<Game2048TileMove>>(emptyList()) }
    var poppedCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    val slideProgress = remember { Animatable(1f) }
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(board) {
        if (slidingMoves.isEmpty()) displayBoard = board
    }

    LaunchedEffect(lastMove) {
        val move = lastMove ?: return@LaunchedEffect
        if (move.tileMoves.isEmpty()) return@LaunchedEffect
        slidingMoves = move.tileMoves
        slideProgress.snapTo(0f)
        slideProgress.animateTo(1f, animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing))
        slidingMoves = emptyList()
        displayBoard = move.board

        val mergedCells = move.tileMoves.filter { it.merged }.map { it.toRow to it.toCol }.toSet()
        if (mergedCells.isNotEmpty()) {
            poppedCells = mergedCells
            popScale.snapTo(1f)
            popScale.animateTo(POP_SCALE, animationSpec = tween(POP_DURATION_MS))
            popScale.animateTo(1f, animationSpec = tween(POP_DURATION_MS))
            poppedCells = emptySet()
        }
    }

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val cellSize = min(maxWidth / board.size, maxHeight / board.size)
        val fontSize = (cellSize.value * 0.38f).sp

        Box(Modifier.size(cellSize * board.size)) {
            if (slidingMoves.isNotEmpty()) {
                for (row in 0 until board.size) {
                    for (col in 0 until board.size) {
                        Box(
                            modifier = Modifier
                                .offset(x = cellSize * col, y = cellSize * row)
                                .size(cellSize - GAP)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (pencil) EMPTY_CELL_COLOR_PENCIL else EMPTY_CELL_COLOR),
                        )
                    }
                }
                slidingMoves.forEach { move ->
                    val row = move.fromRow + (move.toRow - move.fromRow) * slideProgress.value
                    val col = move.fromCol + (move.toCol - move.fromCol) * slideProgress.value
                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize - GAP)
                            .clip(RoundedCornerShape(6.dp))
                            .background(tileColor(move.value, pencil)),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = move.value.toString(),
                            style = TextStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = tileTextColor(move.value, pencil),
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            } else {
                for (row in 0 until board.size) {
                    for (col in 0 until board.size) {
                        val value = displayBoard.tiles[row][col]
                        val isPopped = (row to col) in poppedCells
                        Box(
                            modifier = Modifier
                                .offset(x = cellSize * col, y = cellSize * row)
                                .size(cellSize - GAP)
                                .then(
                                    if (isPopped) {
                                        Modifier.graphicsLayer(scaleX = popScale.value, scaleY = popScale.value)
                                    } else {
                                        Modifier
                                    },
                                )
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
