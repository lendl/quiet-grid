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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val GAP = 2.dp
private val SPARK_COLOR = Color(0xFFF59E0B)

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

            val isPencilTheme = LocalIsPencilTheme.current
            val palette = minesweeperPaletteFor(MaterialTheme.colorScheme.surface)
            val hiddenBase = if (isPencilTheme) MaterialTheme.colorScheme.surfaceVariant else palette.hidden
            val hiddenAlt = if (isPencilTheme) {
                lerp(hiddenBase, MaterialTheme.colorScheme.onSurfaceVariant, 0.08f)
            } else {
                lerp(hiddenBase, MaterialTheme.colorScheme.primary, 0.08f)
            }
            val openedColor = if (isPencilTheme) MaterialTheme.colorScheme.surface else palette.opened
            fun numberColor(count: Int): Color =
                (if (isPencilTheme) PENCIL_NUMBER_COLORS[count] else palette.numbers.getOrNull(count - 1)) ?: palette.numbers.last()

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
                        revealed -> openedColor
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
                            .clip(RoundedCornerShape(percent = 20))
                            .background(backgroundColor)
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
                                bannerColor = MaterialTheme.colorScheme.primary,
                            )
                            cell.state == MinesweeperCellState.REVEALED && cell.isMine -> MinesweeperMineGlyph(
                                bodyColor = MaterialTheme.colorScheme.onErrorContainer,
                                fuseColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            cell.state == MinesweeperCellState.REVEALED && cell.adjacentMines > 0 -> BasicText(
                                text = cell.adjacentMines.toString(),
                                style = TextStyle(
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = numberColor(cell.adjacentMines),
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
private fun MinesweeperFlagGlyph(poleColor: Color, bannerColor: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        val u = size.minDimension / 24f
        drawRoundRect(
            color = poleColor,
            topLeft = Offset(6.2f * u, 3f * u),
            size = Size(1.8f * u, 17f * u),
            cornerRadius = CornerRadius(0.9f * u),
        )
        val banner = Path().apply {
            moveTo(8f * u, 4f * u)
            lineTo(19.6f * u, 4f * u)
            lineTo(16.7f * u, 8.5f * u)
            lineTo(19.6f * u, 13f * u)
            lineTo(8f * u, 13f * u)
            close()
        }
        drawPath(banner, color = bannerColor)
        drawCircle(color = poleColor, radius = 1.7f * u, center = Offset(7.1f * u, 20.6f * u))
    }
}

@Composable
private fun MinesweeperMineGlyph(bodyColor: Color, fuseColor: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        val u = size.minDimension / 24f
        drawCircle(color = bodyColor, radius = 7f * u, center = Offset(10f * u, 14.5f * u))
        rotate(degrees = 45f, pivot = Offset(15f * u, 9.2f * u)) {
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(12.5f * u, 7.8f * u),
                size = Size(5f * u, 2.8f * u),
                cornerRadius = CornerRadius(0.9f * u),
            )
        }
        val fuse = Path().apply {
            moveTo(16.2f * u, 8f * u)
            quadraticTo(18.4f * u, 4.4f * u, 20.6f * u, 6f * u)
        }
        drawPath(fuse, color = fuseColor, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
        val spark = Path().apply {
            moveTo(21f * u, 3f * u)
            lineTo(21.7f * u, 4.5f * u)
            lineTo(23.2f * u, 5.2f * u)
            lineTo(21.7f * u, 5.9f * u)
            lineTo(21f * u, 7.4f * u)
            lineTo(20.3f * u, 5.9f * u)
            lineTo(18.8f * u, 5.2f * u)
            lineTo(20.3f * u, 4.5f * u)
            close()
        }
        drawPath(spark, color = SPARK_COLOR)
    }
}
