package com.quietgrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.badges.BadgeDef
import com.quietgrid.app.core.badges.BadgeIcon
import com.quietgrid.app.core.badges.BadgeSection
import com.quietgrid.app.core.badges.JourneyStage
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class BadgeHue(val dark: Color, val light: Color)

private val GREEN = BadgeHue(Color(0xFFB8E986), Color(0xFF4E8A1F))
private val GOLD = BadgeHue(Color(0xFFF2C879), Color(0xFF9A6A0A))
private val MINT = BadgeHue(Color(0xFF7EE0C3), Color(0xFF1E8A6C))
private val PINK = BadgeHue(Color(0xFFF5A3C7), Color(0xFFB23E74))
private val LAVENDER = BadgeHue(Color(0xFFDAB9FF), Color(0xFF7444B8))
private val SKY = BadgeHue(Color(0xFF9CC7FF), Color(0xFF2F6DB8))
private val GAME_CYCLE = listOf(LAVENDER, MINT, GOLD, PINK, SKY, GREEN)
private val GAME_ORDER = GameCatalog.games.filterNot { it.beta }.map { it.id }

private const val BOX_WIDTH = 100f
private const val BOX_HEIGHT = 112f
private const val CENTER_X = 50f
private const val CENTER_Y = 54f
private const val INNER_SCALE = 0.78f
private const val RING_SCALE = 1.1f
private const val DOT_RADIUS = 47f
private const val DOT_SIZE = 3.6f
private const val STAR_RADIUS = 6.5f
private const val GLYPH_FRACTION = 0.4f
private const val NOT_YET_ALPHA = 0.35f
private const val EARNED_FILL_ALPHA = 0.18f
private const val GLOW_ALPHA = 0.35f
private val HEX_POINTS = listOf(50f to 10f, 88f to 32f, 88f to 76f, 50f to 98f, 12f to 76f, 12f to 32f)
private val STAR_SPOTS = listOf(31f to 23f, 50f to 13f, 69f to 23f)
private val REFERENCE_SIZE = 64.dp
private val MIN_STROKE = 1.5.dp
private val FULL_STROKE = 3.dp

private fun BadgeDef.hue(): BadgeHue = when {
    icon == BadgeIcon.Journey(JourneyStage.EVERY_STAR) -> GOLD
    section != null -> when (section) {
        BadgeSection.ACROSS -> GREEN
        BadgeSection.JOURNEYS -> GOLD
        BadgeSection.PATIENCE -> MINT
        BadgeSection.SEASONAL -> PINK
    }
    else -> GAME_CYCLE[GAME_ORDER.indexOf(gameId).coerceAtLeast(0) % GAME_CYCLE.size]
}

@Composable
fun badgeAccent(def: BadgeDef): Color = when {
    LocalIsPencilTheme.current -> MaterialTheme.colorScheme.onSurface
    LocalIsDarkTheme.current -> def.hue().dark
    else -> def.hue().light
}

private fun hexPath(scale: Float, unit: Float): Path = Path().apply {
    HEX_POINTS.forEachIndexed { index, (x, y) ->
        val px = (CENTER_X + (x - CENTER_X) * scale) * unit
        val py = (CENTER_Y + (y - CENTER_Y) * scale) * unit
        if (index == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

private fun starPath(cx: Float, cy: Float, radius: Float): Path = Path().apply {
    for (index in 0 until 10) {
        val angle = -PI / 2 + index * PI / 5
        val r = if (index % 2 == 0) radius else radius * 0.45f
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private class HexColors(val fill: Color, val line: Color, val glow: Color?, val dotOn: Color, val dotOff: Color)

@Composable
private fun hexColors(def: BadgeDef, earned: Boolean): HexColors {
    val scheme = MaterialTheme.colorScheme
    val pencil = LocalIsPencilTheme.current
    val accent = badgeAccent(def)
    return when {
        pencil && earned -> HexColors(scheme.onSurface.copy(alpha = 0.08f), scheme.onSurface, null, scheme.onSurface, scheme.outlineVariant)
        pencil -> HexColors(scheme.surfaceContainerLowest, scheme.outlineVariant, null, scheme.onSurface, scheme.outlineVariant)
        earned -> HexColors(
            accent.copy(alpha = EARNED_FILL_ALPHA),
            accent,
            accent.copy(alpha = GLOW_ALPHA).takeIf { LocalIsDarkTheme.current },
            accent,
            scheme.outlineVariant,
        )
        else -> HexColors(scheme.surfaceContainerLowest, scheme.onSurfaceVariant.copy(alpha = NOT_YET_ALPHA), null, scheme.onSurfaceVariant, scheme.outlineVariant)
    }
}

@Composable
fun BadgeHex(
    def: BadgeDef,
    earned: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    dots: List<Boolean>? = null,
) {
    val colors = hexColors(def, earned)
    val stroke = maxOf(MIN_STROKE, FULL_STROKE * (size / REFERENCE_SIZE))
    val icon = def.icon
    Box(modifier.size(width = size, height = size * (BOX_HEIGHT / BOX_WIDTH)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = this.size.width / BOX_WIDTH
            drawFrame(colors, unit, stroke.toPx(), icon)
            if (dots != null) drawDots(dots, colors, unit)
        }
        val contentOffset = -(size * ((BOX_HEIGHT / 2 - CENTER_Y) / BOX_WIDTH))
        when (icon) {
            is BadgeIcon.Number -> {
                val digits = icon.value.toString().length
                val fontSize = with(LocalDensity.current) { (size * if (digits > 2) 0.21f else 0.3f).toSp() }
                Text(
                    icon.value.toString(),
                    color = colors.line,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    maxLines = 1,
                    modifier = Modifier.offset(y = contentOffset),
                )
            }
            is BadgeIcon.Glyph -> Icon(
                badgeGlyphIcon(icon.glyph),
                contentDescription = null,
                tint = colors.line,
                modifier = Modifier.offset(y = contentOffset).size(size * GLYPH_FRACTION),
            )
            is BadgeIcon.Journey -> Icon(
                journeyStageIcon(icon.stage),
                contentDescription = null,
                tint = colors.line,
                modifier = Modifier.offset(y = contentOffset + if (icon.stage == JourneyStage.EVERY_STAR) size * 0.04f else 0.dp).size(size * GLYPH_FRACTION),
            )
        }
    }
}

private fun DrawScope.drawFrame(colors: HexColors, unit: Float, strokePx: Float, icon: BadgeIcon) {
    val center = Offset(CENTER_X * unit, CENTER_Y * unit)
    colors.glow?.let { glow ->
        drawCircle(
            brush = Brush.radialGradient(listOf(glow, Color.Transparent), center = center, radius = BOX_WIDTH * 0.62f * unit),
            radius = BOX_WIDTH * 0.62f * unit,
            center = center,
        )
    }
    val outer = hexPath(1f, unit)
    drawPath(outer, colors.fill)
    drawPath(outer, colors.line, style = Stroke(width = strokePx, join = StrokeJoin.Round))
    drawPath(hexPath(INNER_SCALE, unit), colors.line.copy(alpha = colors.line.alpha * 0.45f), style = Stroke(width = strokePx * 0.4f, join = StrokeJoin.Round))
    if (icon == BadgeIcon.Journey(JourneyStage.JOURNEY_END)) {
        drawPath(hexPath(RING_SCALE, unit), colors.line.copy(alpha = colors.line.alpha * 0.7f), style = Stroke(width = strokePx * 0.5f, join = StrokeJoin.Round))
    }
    if (icon == BadgeIcon.Journey(JourneyStage.EVERY_STAR)) {
        STAR_SPOTS.forEach { (x, y) -> drawPath(starPath(x * unit, y * unit, STAR_RADIUS * unit), colors.line) }
    }
}

private fun DrawScope.drawDots(dots: List<Boolean>, colors: HexColors, unit: Float) {
    if (dots.isEmpty()) return
    val step = 2 * PI / dots.size
    dots.forEachIndexed { index, lit ->
        val angle = -PI / 2 + step / 2 + index * step
        val x = (CENTER_X + DOT_RADIUS * cos(angle)).toFloat() * unit
        val y = (CENTER_Y + DOT_RADIUS * sin(angle)).toFloat() * unit
        drawCircle(if (lit) colors.dotOn else colors.dotOff, radius = DOT_SIZE * unit, center = Offset(x, y))
    }
}
