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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
private val PUMPKIN_COLOR = Color(0xFFE8893F)
private val PUMPKIN_STEM_COLOR = Color(0xFF6E8B4E)
private val CANDLE_WAX_COLOR = Color(0xFFF3E6C8)
private val FLAME_CORE_COLOR = Color(0xFFFFE3A3)

internal class MinesweeperHalloweenColors(
    val pumpkin: Color,
    val stem: Color,
    val carve: Color,
    val wax: Color,
    val waxEdge: Color,
    val flame: Color,
    val flameCore: Color,
)

@Composable
internal fun minesweeperHalloweenColors(): MinesweeperHalloweenColors {
    val colors = MaterialTheme.colorScheme
    return if (LocalIsPencilTheme.current) {
        MinesweeperHalloweenColors(
            pumpkin = colors.onErrorContainer,
            stem = colors.onSurfaceVariant,
            carve = colors.errorContainer,
            wax = colors.surface,
            waxEdge = colors.onSurface,
            flame = colors.onSurfaceVariant,
            flameCore = colors.surface,
        )
    } else {
        MinesweeperHalloweenColors(
            pumpkin = PUMPKIN_COLOR,
            stem = PUMPKIN_STEM_COLOR,
            carve = colors.errorContainer,
            wax = CANDLE_WAX_COLOR,
            waxEdge = colors.onSurface.copy(alpha = 0.55f),
            flame = SPARK_COLOR,
            flameCore = FLAME_CORE_COLOR,
        )
    }
}

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
    halloween: Boolean = false,
) {
    val halloweenColors = minesweeperHalloweenColors().takeIf { halloween }
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
                            halloweenColors != null && cell.state == MinesweeperCellState.FLAGGED -> MinesweeperCandleGlyph(halloweenColors)
                            halloweenColors != null && cell.state == MinesweeperCellState.REVEALED && cell.isMine -> MinesweeperPumpkinGlyph(halloweenColors)
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
internal fun MinesweeperFlagGlyph(poleColor: Color, bannerColor: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        drawMinesweeperFlag(Offset.Zero, size, poleColor, bannerColor)
    }
}

@Composable
private fun MinesweeperMineGlyph(bodyColor: Color, fuseColor: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        drawMinesweeperMine(Offset.Zero, size, bodyColor, fuseColor)
    }
}

@Composable
private fun MinesweeperCandleGlyph(colors: MinesweeperHalloweenColors) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        drawMinesweeperCandle(Offset.Zero, size, colors)
    }
}

@Composable
private fun MinesweeperPumpkinGlyph(colors: MinesweeperHalloweenColors) {
    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
        drawMinesweeperPumpkin(Offset.Zero, size, colors)
    }
}

internal fun DrawScope.drawMinesweeperCandle(topLeft: Offset, size: Size, colors: MinesweeperHalloweenColors) {
    val u = size.minDimension / 24f
    fun at(x: Float, y: Float) = Offset(topLeft.x + x * u, topLeft.y + y * u)
    drawRoundRect(colors.waxEdge, at(5f, 19.6f), Size(14f * u, 2.4f * u), CornerRadius(1.2f * u))
    drawRoundRect(colors.wax, at(8.5f, 9f), Size(7f * u, 11f * u), CornerRadius(1.2f * u))
    drawRoundRect(colors.waxEdge, at(8.5f, 9f), Size(7f * u, 11f * u), CornerRadius(1.2f * u), style = Stroke(width = u))
    drawLine(colors.waxEdge, at(12f, 9f), at(12f, 7.2f), strokeWidth = 0.9f * u, cap = StrokeCap.Round)
    val flame = Path().apply {
        at(12f, 1.4f).let { moveTo(it.x, it.y) }
        val right = at(15.6f, 5.6f)
        val bottom = at(12f, 7.8f)
        val left = at(8.4f, 5.6f)
        val top = at(12f, 1.4f)
        quadraticTo(right.x, right.y, bottom.x, bottom.y)
        quadraticTo(left.x, left.y, top.x, top.y)
        close()
    }
    drawPath(flame, colors.flame)
    drawOval(colors.flameCore, at(10.9f, 4.6f), Size(2.2f * u, 3f * u))
}

internal fun DrawScope.drawMinesweeperPumpkin(topLeft: Offset, size: Size, colors: MinesweeperHalloweenColors) {
    val u = size.minDimension / 24f
    fun at(x: Float, y: Float) = Offset(topLeft.x + x * u, topLeft.y + y * u)
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { index, (x, y) ->
            val point = at(x, y)
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawRoundRect(colors.stem, at(10.8f, 2.6f), Size(2.4f * u, 5.4f * u), CornerRadius(1f * u))
    drawOval(colors.pumpkin, at(2.5f, 7f), Size(11f * u, 14.5f * u))
    drawOval(colors.pumpkin, at(10.5f, 7f), Size(11f * u, 14.5f * u))
    drawOval(colors.pumpkin, at(6.5f, 6.4f), Size(11f * u, 15.6f * u))
    drawPath(polygon(7.4f to 13.8f, 11f to 13.8f, 9.2f to 10.6f), colors.carve)
    drawPath(polygon(13f to 13.8f, 16.6f to 13.8f, 14.8f to 10.6f), colors.carve)
    drawPath(polygon(7f to 15.6f, 17f to 15.6f, 15.4f to 18.2f, 12f to 19.2f, 8.6f to 18.2f), colors.carve)
}

internal fun DrawScope.drawMinesweeperFlag(topLeft: Offset, size: Size, poleColor: Color, bannerColor: Color) {
    val u = size.minDimension / 24f
    fun at(x: Float, y: Float) = Offset(topLeft.x + x * u, topLeft.y + y * u)
    drawRoundRect(
        color = poleColor,
        topLeft = at(6.2f, 3f),
        size = Size(1.8f * u, 17f * u),
        cornerRadius = CornerRadius(0.9f * u),
    )
    val banner = Path().apply {
        at(8f, 4f).let { moveTo(it.x, it.y) }
        at(19.6f, 4f).let { lineTo(it.x, it.y) }
        at(16.7f, 8.5f).let { lineTo(it.x, it.y) }
        at(19.6f, 13f).let { lineTo(it.x, it.y) }
        at(8f, 13f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(banner, color = bannerColor)
    drawCircle(color = poleColor, radius = 1.7f * u, center = at(7.1f, 20.6f))
}

internal fun DrawScope.drawMinesweeperMine(topLeft: Offset, size: Size, bodyColor: Color, fuseColor: Color) {
    val u = size.minDimension / 24f
    fun at(x: Float, y: Float) = Offset(topLeft.x + x * u, topLeft.y + y * u)
    drawCircle(color = bodyColor, radius = 7f * u, center = at(10f, 14.5f))
    rotate(degrees = 45f, pivot = at(15f, 9.2f)) {
        drawRoundRect(
            color = bodyColor,
            topLeft = at(12.5f, 7.8f),
            size = Size(5f * u, 2.8f * u),
            cornerRadius = CornerRadius(0.9f * u),
        )
    }
    val fuse = Path().apply {
        at(16.2f, 8f).let { moveTo(it.x, it.y) }
        val control = at(18.4f, 4.4f)
        val end = at(20.6f, 6f)
        quadraticTo(control.x, control.y, end.x, end.y)
    }
    drawPath(fuse, color = fuseColor, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
    val spark = Path().apply {
        at(21f, 3f).let { moveTo(it.x, it.y) }
        at(21.7f, 4.5f).let { lineTo(it.x, it.y) }
        at(23.2f, 5.2f).let { lineTo(it.x, it.y) }
        at(21.7f, 5.9f).let { lineTo(it.x, it.y) }
        at(21f, 7.4f).let { lineTo(it.x, it.y) }
        at(20.3f, 5.9f).let { lineTo(it.x, it.y) }
        at(18.8f, 5.2f).let { lineTo(it.x, it.y) }
        at(20.3f, 4.5f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(spark, color = SPARK_COLOR)
}
