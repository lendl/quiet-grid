package com.quietgrid.app.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.GameId
import com.quietgrid.app.nav.LocalAnimatedVisibilityScope
import com.quietgrid.app.nav.LocalSharedTransitionScope
import com.quietgrid.app.ui.components.SharedElementKeys
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.playSizeClass

class ResultLink(val labelRes: Int, val onClick: () -> Unit)

fun isSidewaysPhoneResult(width: Dp, height: Dp): Boolean =
    playSizeClass(width, height) == PlaySizeClass.PhoneLandscape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ResultMedallion(
    gameId: GameId,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    shared: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedModifier = if (shared && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SharedElementKeys.gameIdentity(gameId)),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    Box(
        sharedModifier
            .size(size)
            .then(modifier)
            .background(tint.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun MedallionSymbol(symbol: String, symbolSize: TextUnit) {
    Text(symbol, fontSize = symbolSize)
}

@Composable
fun DrawnCheck(progress: () -> Float, tint: Color, modifier: Modifier = Modifier) {
    val measure = remember { PathMeasure().apply { setPath(glyphPath(CHECK_POINTS), false) } }
    val segment = remember { Path() }
    Canvas(modifier) {
        val unit = size.minDimension / GLYPH_VIEWPORT
        segment.reset()
        measure.getSegment(0f, measure.length * progress(), segment, true)
        scale(unit, unit, pivot = Offset.Zero) {
            drawPath(segment, tint, style = glyphStroke())
        }
    }
}

@Composable
fun LossGlyph(abandoned: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val lines = if (abandoned) PAUSE_LINES else CROSS_LINES
    Canvas(modifier) {
        val unit = size.minDimension / GLYPH_VIEWPORT
        lines.forEach { (start, end) ->
            drawLine(
                tint,
                start = Offset(start.x * unit, start.y * unit),
                end = Offset(end.x * unit, end.y * unit),
                strokeWidth = GLYPH_STROKE * unit,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val GLYPH_VIEWPORT = 24f
private const val GLYPH_STROKE = 2.2f
private val CHECK_POINTS = listOf(Offset(6f, 12.5f), Offset(10f, 16.5f), Offset(18f, 7.5f))
private val PAUSE_LINES = listOf(Offset(9f, 6f) to Offset(9f, 18f), Offset(15f, 6f) to Offset(15f, 18f))
private val CROSS_LINES = listOf(Offset(7f, 7f) to Offset(17f, 17f), Offset(17f, 7f) to Offset(7f, 17f))

private fun glyphPath(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
}

private fun glyphStroke(): Stroke = Stroke(width = GLYPH_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun CompactResultActions(
    links: List<ResultLink>,
    modifier: Modifier = Modifier,
    primary: @Composable () -> Unit,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResultLinkGroup(links.dropLast(1))
        primary()
        ResultLinkGroup(links.takeLast(1))
    }
}

@Composable
fun ResultLinkGroup(links: List<ResultLink>) {
    if (links.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        links.forEachIndexed { index, link ->
            if (index > 0) {
                Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
            TextButton(onClick = link.onClick) {
                Text(stringResource(link.labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ResultMetaDivider(height: Dp = 28.dp) {
    Box(Modifier.width(1.dp).height(height).background(MaterialTheme.colorScheme.outlineVariant))
}
