package com.quietgrid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.components.rememberReduceMotion
import com.quietgrid.app.ui.play.FoldHandoff
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MEDAL_START_MS = 167f
private const val GROW_MS = 600f
private const val GLIDE_MS = 733f
private const val LANDED_MS = MEDAL_START_MS + GROW_MS + GLIDE_MS
private const val GLOW_START_MS = 667f
private const val GLOW_MS = 1333f
private const val CHECK_START_MS = 750f
private const val CHECK_MS = 750f
private const val ROWS_START_MS = 1250f
private const val ROW_MS = 667f
private const val ROW_STAGGER_MS = 100f
private const val MAX_ROW_INDEX = 10
private const val SCORE_START_MS = 1550f
private const val SCORE_MS = 1083f
private const val BADGES_START_MS = 1950f
private const val BADGES_MS = 500f
private const val ENTRANCE_END_MS = ROWS_START_MS + ROW_STAGGER_MS * MAX_ROW_INDEX + ROW_MS
private const val GROW_FROM_SCALE = 0.25f
private val ROW_RISE = 10.dp

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val StandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@Stable
class ResultEntrance internal constructor(private val time: () -> Float, private val origin: Offset?) {
    internal var heroCenter by mutableStateOf<Offset?>(null)

    fun landed(): Boolean = time() >= LANDED_MS

    fun medalAlpha(): Float = EmphasizedEasing.transform(fraction(MEDAL_START_MS, GROW_MS))

    fun medalScale(): Float = GROW_FROM_SCALE + (1f - GROW_FROM_SCALE) * medalAlpha()

    fun glowAlpha(): Float = fraction(GLOW_START_MS, GLOW_MS)

    fun checkProgress(): Float = StandardEasing.transform(fraction(CHECK_START_MS, CHECK_MS))

    fun rowProgress(index: Int): Float =
        EmphasizedEasing.transform(fraction(ROWS_START_MS + index.coerceIn(0, MAX_ROW_INDEX) * ROW_STAGGER_MS, ROW_MS))

    fun scoreProgress(): Float = 1f - (1f - fraction(SCORE_START_MS, SCORE_MS)).pow(3)

    fun badgesAlpha(): Float = fraction(BADGES_START_MS, BADGES_MS)

    internal fun flightCenter(): Offset? {
        val target = heroCenter ?: return null
        val from = origin ?: return target
        return lerp(from, target, StandardEasing.transform(fraction(MEDAL_START_MS + GROW_MS, GLIDE_MS)))
    }

    private fun fraction(start: Float, duration: Float): Float = ((time() - start) / duration).coerceIn(0f, 1f)
}

@Composable
fun rememberResultEntrance(): ResultEntrance {
    val reduceMotion = rememberReduceMotion()
    var played by rememberSaveable { mutableStateOf(false) }
    val origin = remember { FoldHandoff.consume() }
    val clock = remember { Animatable(if (reduceMotion || played) ENTRANCE_END_MS else 0f) }
    LaunchedEffect(Unit) {
        val remaining = (ENTRANCE_END_MS - clock.value).roundToInt()
        clock.animateTo(ENTRANCE_END_MS, tween(remaining, easing = LinearEasing))
        played = true
    }
    return remember { ResultEntrance({ clock.value }, origin.takeUnless { reduceMotion }) }
}

fun Modifier.riseIn(entrance: ResultEntrance, index: Int): Modifier = graphicsLayer {
    val progress = entrance.rowProgress(index)
    alpha = progress
    translationY = (1f - progress) * ROW_RISE.toPx()
}

fun Modifier.badgesFadeIn(entrance: ResultEntrance): Modifier = graphicsLayer { alpha = entrance.badgesAlpha() }

fun Modifier.heroSlot(entrance: ResultEntrance): Modifier = this
    .onGloballyPositioned { coordinates ->
        entrance.heroCenter = coordinates.positionInRoot() + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
    }
    .graphicsLayer { alpha = if (entrance.landed()) 1f else 0f }

@Composable
fun BoxScope.ResultHeroFlight(entrance: ResultEntrance, hero: @Composable () -> Unit) {
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .matchParentSize()
            .clearAndSetSemantics {}
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
    ) {
        Box(
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    val center = entrance.flightCenter()
                    if (center != null && !entrance.landed()) {
                        placeable.placeWithLayer(
                            (center.x - overlayOrigin.x - placeable.width / 2f).roundToInt(),
                            (center.y - overlayOrigin.y - placeable.height / 2f).roundToInt(),
                        ) {
                            val scale = entrance.medalScale()
                            scaleX = scale
                            scaleY = scale
                            alpha = entrance.medalAlpha()
                        }
                    }
                }
            },
        ) {
            hero()
        }
    }
}
