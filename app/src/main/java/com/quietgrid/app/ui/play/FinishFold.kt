package com.quietgrid.app.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.quietgrid.app.session.FinishKind
import com.quietgrid.app.session.FinishMotion
import com.quietgrid.app.ui.components.rememberReduceMotion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CONTENT_FADE_MS = 633f
private const val CHROME_FADE_START_MS = 167f
private const val CHROME_FADE_MS = 500f
private const val TILE_START_MS = 83f
private const val TILE_STAGGER_MS = 300f
private const val TILE_MS = 1417f
private const val FOLD_END_MS = TILE_START_MS + TILE_STAGGER_MS + TILE_MS
private const val SHRINK_END = 0.42f
private const val SLIDE_END = 0.94f
private const val SHRUNK_SCALE = 0.5f
private const val SLID_SCALE = 0.36f
private const val FINAL_SCALE = 0.34f
private const val SLID_ALPHA = 0.6f
private const val TILE_CORNER_FRACTION = 0.15f
private const val DEFAULT_FOLD_TILES = 9
private val DOCK_DROP = 16.dp

private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val TileEase = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val SlideEase = CubicBezierEasing(0.5f, 0f, 0.9f, 0.6f)

@Stable
internal class FoldState(private val time: () -> Float, val active: Boolean) {
    fun contentAlpha(): Float = if (active) 1f - EaseIn.transform(fraction(time(), 0f, CONTENT_FADE_MS)) else 1f

    fun chromeAlpha(): Float = if (active) 1f - EaseIn.transform(fraction(time(), CHROME_FADE_START_MS, CHROME_FADE_MS)) else 1f

    fun tileProgress(delayMs: Float): Float = TileEase.transform(fraction(time(), TILE_START_MS + delayMs, TILE_MS))
}

private fun fraction(time: Float, start: Float, duration: Float): Float = ((time - start) / duration).coerceIn(0f, 1f)

@Composable
internal fun rememberFoldState(finishing: FinishKind?): FoldState {
    val reduceMotion = rememberReduceMotion()
    SideEffect { FinishMotion.reduced = reduceMotion }
    val clock = remember { Animatable(0f) }
    val active = finishing != null && !reduceMotion
    LaunchedEffect(active) {
        if (active) clock.animateTo(FOLD_END_MS, tween(FOLD_END_MS.roundToInt(), easing = LinearEasing))
    }
    return remember(active) { FoldState({ clock.value }, active) }
}

internal fun foldGrid(rows: Int?, cols: Int?, aspectRatio: Float): Pair<Int, Int> {
    val aspect = aspectRatio.takeIf { it > 0f } ?: 1f
    val foldRows = rows
        ?: cols?.let { max(1, (it / aspect).roundToInt()) }
        ?: if (aspect >= 1f) DEFAULT_FOLD_TILES else max(1, (DEFAULT_FOLD_TILES / aspect).roundToInt())
    val foldCols = cols ?: max(1, (foldRows * aspect).roundToInt())
    return foldRows to foldCols
}

internal fun Modifier.foldChrome(fold: FoldState): Modifier = graphicsLayer { alpha = fold.chromeAlpha() }

internal fun Modifier.foldDock(fold: FoldState): Modifier = graphicsLayer {
    val visible = fold.chromeAlpha()
    alpha = visible
    translationY = (1f - visible) * DOCK_DROP.toPx()
}

@Composable
internal fun FinishFold(
    fold: FoldState,
    finishing: FinishKind?,
    rows: Int,
    cols: Int,
    content: @Composable BoxScope.() -> Unit,
) {
    var center by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(finishing) {
        if (finishing != null) FoldHandoff.set(center)
    }
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            center = coordinates.positionInRoot() + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
        },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = fold.contentAlpha() },
            contentAlignment = Alignment.Center,
            content = content,
        )
        if (fold.active) {
            val startColor = MaterialTheme.colorScheme.surface
            val endColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(Modifier.fillMaxSize()) { drawFoldTiles(fold, rows, cols, startColor, endColor) }
        }
    }
}

private fun DrawScope.drawFoldTiles(fold: FoldState, rows: Int, cols: Int, startColor: Color, endColor: Color) {
    val aspect = cols.toFloat() / rows
    val gridWidth = min(size.width, size.height * aspect)
    val gridHeight = gridWidth / aspect
    val left = (size.width - gridWidth) / 2f
    val top = (size.height - gridHeight) / 2f
    val tileWidth = gridWidth / cols
    val tileHeight = gridHeight / rows
    val target = Offset(size.width / 2f, size.height / 2f)
    val reveal = 1f - fold.contentAlpha()
    val midRow = (rows - 1) / 2f
    val midCol = (cols - 1) / 2f
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val ring = max(spread(row, midRow), spread(col, midCol))
            val progress = fold.tileProgress((1f - ring) * TILE_STAGGER_MS)
            val tint: Float
            val scale: Float
            val travel: Float
            val alpha: Float
            when {
                progress <= SHRINK_END -> {
                    tint = progress / SHRINK_END
                    scale = 1f + (SHRUNK_SCALE - 1f) * tint
                    travel = 0f
                    alpha = 1f
                }
                progress <= SLIDE_END -> {
                    val slide = SlideEase.transform((progress - SHRINK_END) / (SLIDE_END - SHRINK_END))
                    tint = 1f
                    scale = SHRUNK_SCALE + (SLID_SCALE - SHRUNK_SCALE) * slide
                    travel = slide
                    alpha = 1f + (SLID_ALPHA - 1f) * slide
                }
                else -> {
                    val fade = (progress - SLIDE_END) / (1f - SLIDE_END)
                    tint = 1f
                    scale = SLID_SCALE + (FINAL_SCALE - SLID_SCALE) * fade
                    travel = 1f
                    alpha = SLID_ALPHA * (1f - fade)
                }
            }
            val home = Offset(left + (col + 0.5f) * tileWidth, top + (row + 0.5f) * tileHeight)
            val at = home + (target - home) * travel
            val width = tileWidth * scale
            val height = tileHeight * scale
            drawRoundRect(
                color = lerp(startColor, endColor, tint),
                topLeft = Offset(at.x - width / 2f, at.y - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(min(width, height) * TILE_CORNER_FRACTION * tint),
                alpha = alpha * reveal,
            )
        }
    }
}

private fun spread(index: Int, mid: Float): Float = if (mid == 0f) 0f else abs(index - mid) / mid
