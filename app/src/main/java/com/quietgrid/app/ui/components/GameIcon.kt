package com.quietgrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.app.ui.theme.PlusJakartaSans

private const val GLYPH_UNITS = 24f
private const val GLYPH_FRACTION = 28f / 44f
private const val CORNER_FRACTION = 12f / 44f
private val PENCIL_TILE_COLOR = Color(0xFFE0E0E0)
private val PENCIL_INK_COLOR = Color(0xFF1A1A1A)
private val TILE_INK_COLOR = Color.White

fun glyphPaintColor(paint: GlyphPaint, tile: Color, ink: Color, isPencilTheme: Boolean): Color {
    fun tint(alpha: Float, pencilAlpha: Float): Color = ink.copy(alpha = if (isPencilTheme) pencilAlpha else alpha).compositeOver(tile)
    return when (paint) {
        GlyphPaint.ACCENT, GlyphPaint.MARK, GlyphPaint.LINE_ACCENT -> ink
        GlyphPaint.CUTOUT -> tile
        GlyphPaint.NEUTRAL -> tint(0.42f, 0.3f)
        GlyphPaint.EMPTY -> tint(0.18f, 0.08f)
        GlyphPaint.SMALL, GlyphPaint.LINE -> tint(0.6f, 0.45f)
    }
}

@Composable
fun GameIcon(gameId: GameId, size: Dp, modifier: Modifier = Modifier) {
    val isPencilTheme = LocalIsPencilTheme.current
    val tile = if (isPencilTheme) PENCIL_TILE_COLOR else GAME_ICON_HUES[gameId] ?: MaterialTheme.colorScheme.primary
    val ink = if (isPencilTheme) PENCIL_INK_COLOR else TILE_INK_COLOR
    val shapes = GAME_ICON_GLYPHS[gameId].orEmpty()
    val paths = remember(gameId) {
        shapes.filterIsInstance<GlyphPath>().associateWith { PathParser().parsePathString(it.pathData).toPath() }
    }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * CORNER_FRACTION))
            .background(tile),
    ) {
        val glyphSize = this.size.minDimension * GLYPH_FRACTION
        val unit = glyphSize / GLYPH_UNITS
        val origin = Offset((this.size.width - glyphSize) / 2, (this.size.height - glyphSize) / 2)

        shapes.forEach { shape ->
            val color = glyphPaintColor(shape.paint, tile, ink, isPencilTheme)
            when (shape) {
                is GlyphText -> {
                    val fontSize = (shape.size * unit).toSp()
                    val layout = textMeasurer.measure(
                        shape.text,
                        TextStyle(
                            color = color,
                            fontSize = fontSize,
                            lineHeight = fontSize,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (shape.letterSpacing * unit).toSp(),
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                        ),
                    )
                    drawText(
                        layout,
                        topLeft = Offset(
                            origin.x + shape.x * unit - layout.size.width / 2f,
                            origin.y + shape.y * unit - layout.size.height / 2f,
                        ),
                    )
                }
                is GlyphRect -> translate(origin.x, origin.y) {
                    scale(unit, unit, pivot = Offset.Zero) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(shape.x, shape.y),
                            size = Size(shape.width, shape.height),
                            cornerRadius = CornerRadius(shape.radius),
                        )
                    }
                }
                is GlyphOval -> translate(origin.x, origin.y) {
                    scale(unit, unit, pivot = Offset.Zero) {
                        drawOval(
                            color = color,
                            topLeft = Offset(shape.cx - shape.rx, shape.cy - shape.ry),
                            size = Size(shape.rx * 2, shape.ry * 2),
                        )
                    }
                }
                is GlyphPath -> translate(origin.x, origin.y) {
                    scale(unit, unit, pivot = Offset.Zero) {
                        val path = paths.getValue(shape)
                        val strokeWidth = shape.strokeWidth
                        if (strokeWidth != null) {
                            drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } else {
                            drawPath(path, color = color)
                        }
                    }
                }
            }
        }
    }
}
