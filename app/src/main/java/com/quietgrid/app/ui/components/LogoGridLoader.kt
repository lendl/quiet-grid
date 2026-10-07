package com.quietgrid.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private const val LOADER_LOOP_MS = 2400
private const val LOADER_STEP_OFFSET_MS = 250
private const val LOADER_GAP_FRACTION = 4f / 44f
private val LOGO_LAVENDER_ON_LIGHT = Color(0xFFDAB9FF)
private val LOGO_LAVENDER_ON_DARK = Color(0xFF7131E3)

@Composable
fun logoLavenderColor(): Color = when {
    LocalIsPencilTheme.current -> MaterialTheme.colorScheme.tertiary
    MaterialTheme.colorScheme.surface.luminance() < 0.5f -> LOGO_LAVENDER_ON_DARK
    else -> LOGO_LAVENDER_ON_LIGHT
}

@Composable
fun LogoGridLoader(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val primary = MaterialTheme.colorScheme.primary
    val lavender = logoLavenderColor()
    val dim = MaterialTheme.colorScheme.surfaceContainerHighest
    val fills = listOf(primary, primary, primary, lavender)
    val progress = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "logo-grid-loader")
        fills.indices.map { index ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = LOADER_LOOP_MS
                        0f at 0
                        0f at (LOADER_LOOP_MS * 0.06f).toInt() using FastOutSlowInEasing
                        1f at (LOADER_LOOP_MS * 0.20f).toInt()
                        1f at (LOADER_LOOP_MS * 0.68f).toInt() using FastOutSlowInEasing
                        0f at (LOADER_LOOP_MS * 0.84f).toInt()
                        0f at LOADER_LOOP_MS
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * LOADER_STEP_OFFSET_MS),
                ),
                label = "logo-grid-square-$index",
            )
        }
    } else {
        null
    }
    val gap = size * LOADER_GAP_FRACTION
    val squareSize = (size - gap) / 2
    val squareShape = RoundedCornerShape(percent = 24)

    @Composable
    fun Square(index: Int) {
        val t = progress?.get(index)?.value ?: 1f
        val scale = 0.86f + 0.14f * t
        Box(
            Modifier
                .size(squareSize)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(squareShape)
                .background(lerp(dim, fills[index], t)),
        )
    }

    Column(modifier.size(size), verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Square(0)
            Square(1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Square(3)
            Square(2)
        }
    }
}
