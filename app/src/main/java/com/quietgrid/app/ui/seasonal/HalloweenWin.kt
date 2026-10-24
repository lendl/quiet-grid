package com.quietgrid.app.ui.seasonal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.isHalloween
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val EYES_OPEN_START_MS = 800f
private const val EYES_OPEN_END_MS = 1300f
private const val SLIT_END_MS = 1500f
private const val LOOK_START_MS = 1300f
private const val LOOK_END_MS = 1900f
private const val BLINK_START_MS = 2000f
private const val BLINK_END_MS = 2280f
private const val BLINK_DEPTH = 0.92f

private val grayscalePaint = Paint().apply {
    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

private fun progress(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun phase(t: Float, start: Float, end: Float): Float = FastOutSlowInEasing.transform(progress(t, start, end))

private fun pulse(t: Float, start: Float, end: Float): Float {
    if (t <= start || t >= end) return 0f
    return 1f - abs(2f * progress(t, start, end) - 1f)
}

@Composable
fun rememberHalloweenActive(): Boolean {
    val today = remember { LocalDate.now() }
    if (!isHalloween(today)) return false
    val repositories: RepositoriesViewModel = hiltViewModel()
    val enabled by remember(repositories) { repositories.settingsRepository.settings.map { it.seasonalTouchesEnabled } }
        .collectAsState(initial = true)
    return enabled
}

@Composable
fun rememberHalloweenCharacter(): HalloweenCharacter? {
    val active = rememberHalloweenActive()
    val character = rememberSaveable { HalloweenCharacter.entries.random() }
    return character.takeIf { active }
}

@Composable
fun HalloweenFigure(character: HalloweenCharacter, size: Dp, clockMs: () -> Float, modifier: Modifier = Modifier) {
    val description = stringResource(character.descriptionRes)
    val pencil = LocalIsPencilTheme.current
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val t = clockMs()
        val open = phase(t, EYES_OPEN_START_MS, EYES_OPEN_END_MS)
        val eyes = HalloweenEyes(
            open = open * (1f - BLINK_DEPTH * pulse(t, BLINK_START_MS, BLINK_END_MS)),
            slit = phase(t, EYES_OPEN_START_MS, SLIT_END_MS),
            look = -sin(2f * PI.toFloat() * progress(t, LOOK_START_MS, LOOK_END_MS)),
        )
        val unit = this.size.width / HALLOWEEN_UNITS
        grayscaleIf(pencil) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawHalloweenBody(character, open)
                drawHalloweenEyes(character, eyes)
            }
        }
    }
}

private inline fun DrawScope.grayscaleIf(enabled: Boolean, block: DrawScope.() -> Unit) {
    if (!enabled) return block()
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), grayscalePaint)
        block()
        canvas.restore()
    }
}
