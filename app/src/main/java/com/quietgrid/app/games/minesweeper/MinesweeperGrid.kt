package com.quietgrid.app.games.minesweeper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.cos
import kotlin.math.sin

private val GAP = 1.dp

private val NUMBER_COLORS = mapOf(
    1 to Color(0xFF1E88E5),
    2 to Color(0xFF43A047),
    3 to Color(0xFFE53935),
    4 to Color(0xFF8E24AA),
    5 to Color(0xFF6D4C41),
    6 to Color(0xFF00ACC1),
    7 to Color(0xFF000000),
    8 to Color(0xFF757575),
)

private val PENCIL_NUMBER_COLORS = mapOf(
    1 to Color(0xFF666666),
    2 to Color(0xFF595959),
    3 to Color(0xFF4D4D4D),
    4 to Color(0xFF404040),
    5 to Color(0xFF333333),
    6 to Color(0xFF262626),
    7 to Color(0xFF000000),
    8 to Color(0xFF0D0D0D),
)

@Composable
private fun minesweeperNumberColor(count: Int): Color {
    val colors = if (LocalIsPencilTheme.current) PENCIL_NUMBER_COLORS else NUMBER_COLORS
    return colors[count] ?: MaterialTheme.colorScheme.onSurface
}

private fun Modifier.tileBevel(raised: Boolean): Modifier = drawBehind {
    val t = (size.minDimension * 0.06f).coerceAtMost(1.5.dp.toPx())
    val hi = Color.White.copy(alpha = if (raised) 0.55f else 0.15f)
    val lo = Color.Black.copy(alpha = if (raised) 0.2f else 0.32f)
    val nearColor = if (raised) hi else lo
    val farColor = if (raised) lo else hi
    drawRect(color = nearColor, size = Size(size.width, t))
    drawRect(color = nearColor, size = Size(t, size.height))
    drawRect(color = farColor, topLeft = Offset(0f, size.height - t), size = Size(size.width, t))
    drawRect(color = farColor, topLeft = Offset(size.width - t, 0f), size = Size(t, size.height))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperGrid(
    board: MinesweeperBoard,
    onReveal: (row: Int, col: Int) -> Unit,
    onToggleFlag: (row: Int, col: Int) -> Unit,
    hintEvidenceCells: Set<Pair<Int, Int>> = emptySet(),
    hintTargetCells: Set<Pair<Int, Int>> = emptySet(),
) {
    Box {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val cellSize = min(maxWidth / board.cols, maxHeight / board.rows)
            val stride = cellSize
            val fontSize = (cellSize.value * 0.42f).sp

            val hiddenBase = MaterialTheme.colorScheme.surfaceVariant
            val hiddenAlt = lerp(hiddenBase, MaterialTheme.colorScheme.onSurfaceVariant, 0.08f)

            Box(Modifier.size(cellSize * board.cols, cellSize * board.rows)) {
            for (row in 0 until board.rows) {
                for (col in 0 until board.cols) {
                    val cell = board.cells[row][col]
                    val cellKey = row to col
                    val isHintTarget = cellKey in hintTargetCells
                    val isHintEvidence = cellKey in hintEvidenceCells
                    val revealed = cell.state == MinesweeperCellState.REVEALED
                    val backgroundColor = when {
                        isHintTarget -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                        isHintEvidence -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        revealed && cell.isMine -> MaterialTheme.colorScheme.errorContainer
                        revealed -> MaterialTheme.colorScheme.surface
                        (row + col) % 2 == 0 -> hiddenBase
                        else -> hiddenAlt
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = stride * col, y = stride * row)
                            .size(cellSize - GAP)
                            .combinedClickable(
                                onClick = { onReveal(row, col) },
                                onLongClick = { onToggleFlag(row, col) },
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(backgroundColor)
                            .tileBevel(raised = !revealed)
                            .then(
                                if (isHintTarget || isHintEvidence) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            cell.state == MinesweeperCellState.FLAGGED -> MinesweeperFlagGlyph(
                                poleColor = MaterialTheme.colorScheme.onSurface,
                                pennantColor = MaterialTheme.colorScheme.error,
                            )
                            cell.state == MinesweeperCellState.REVEALED && cell.isMine -> MinesweeperMineGlyph(
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            cell.state == MinesweeperCellState.REVEALED && cell.adjacentMines > 0 -> BasicText(
                                text = cell.adjacentMines.toString(),
                                style = TextStyle(
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = minesweeperNumberColor(cell.adjacentMines),
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            else -> Unit
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MinesweeperFlagGlyph(poleColor: Color, pennantColor: Color) {
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        drawLine(
            color = poleColor,
            start = Offset(size.width * 0.32f, size.height * 0.1f),
            end = Offset(size.width * 0.32f, size.height * 0.86f),
            strokeWidth = size.width * 0.09f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = poleColor,
            start = Offset(size.width * 0.18f, size.height * 0.86f),
            end = Offset(size.width * 0.5f, size.height * 0.86f),
            strokeWidth = size.width * 0.09f,
            cap = StrokeCap.Round,
        )
        val pennant = Path().apply {
            moveTo(size.width * 0.36f, size.height * 0.12f)
            lineTo(size.width * 0.86f, size.height * 0.28f)
            lineTo(size.width * 0.36f, size.height * 0.46f)
            close()
        }
        drawPath(pennant, color = pennantColor)
    }
}

private val MINE_CARDINAL_ANGLES = listOf(0, 90, 180, 270)
private val MINE_DIAGONAL_ANGLES = listOf(45, 135, 225, 315)

@Composable
private fun MinesweeperMineGlyph(color: Color) {
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        val center = Offset(size.width * 0.46f, size.height * 0.52f)
        val bodyRadius = size.minDimension / 2f * 0.42f
        val cardinalLength = size.minDimension / 2f * 0.88f
        val diagonalLength = size.minDimension / 2f * 0.66f
        val cardinalWidth = size.minDimension * 0.09f
        val diagonalWidth = size.minDimension * 0.05f

        fun spikeEnd(angleDegrees: Int, length: Float): Offset {
            val angle = Math.toRadians(angleDegrees.toDouble())
            return Offset(center.x + (cos(angle) * length).toFloat(), center.y + (sin(angle) * length).toFloat())
        }

        for (angle in MINE_DIAGONAL_ANGLES) {
            drawLine(color = color, start = center, end = spikeEnd(angle, diagonalLength), strokeWidth = diagonalWidth, cap = StrokeCap.Round)
        }
        for (angle in MINE_CARDINAL_ANGLES) {
            val tip = spikeEnd(angle, cardinalLength)
            drawLine(color = color, start = center, end = tip, strokeWidth = cardinalWidth, cap = StrokeCap.Round)
            val capAngle = Math.toRadians((angle + 90).toDouble())
            val capHalf = cardinalWidth * 0.9f
            drawLine(
                color = color,
                start = Offset(tip.x - (cos(capAngle) * capHalf).toFloat(), tip.y - (sin(capAngle) * capHalf).toFloat()),
                end = Offset(tip.x + (cos(capAngle) * capHalf).toFloat(), tip.y + (sin(capAngle) * capHalf).toFloat()),
                strokeWidth = cardinalWidth * 0.7f,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(color = color, radius = bodyRadius, center = center)

        val fuse = Path().apply {
            moveTo(center.x + bodyRadius * 0.35f, center.y - bodyRadius * 0.85f)
            quadraticTo(
                center.x + bodyRadius * 1.5f, center.y - bodyRadius * 1.9f,
                center.x + bodyRadius * 2.15f, center.y - bodyRadius * 1.55f,
            )
        }
        drawPath(fuse, color = color, style = Stroke(width = cardinalWidth * 0.55f, cap = StrokeCap.Round))
        drawCircle(
            color = Color(0xFFFFC107),
            radius = bodyRadius * 0.22f,
            center = Offset(center.x + bodyRadius * 2.15f, center.y - bodyRadius * 1.55f),
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = bodyRadius * 0.32f,
            center = Offset(center.x - bodyRadius * 0.32f, center.y - bodyRadius * 0.32f),
        )
    }
}
