package com.quietgrid.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

/**
 * A cell value that spins once when [isCorrect] flips true (a completed line/unit matched the
 * solution) and shakes once when [isIncorrect] flips true (it didn't). Mirrors the RN app's
 * per-cell spin/shake board feedback instead of a static border-color flash alone.
 */
@Composable
fun FeedbackText(
    text: String,
    style: TextStyle,
    isCorrect: Boolean,
    isIncorrect: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    val shakeX = remember { Animatable(0f) }

    // isCorrect/isIncorrect get cleared by the caller ~500ms after they flip true (to end the
    // border-color flash). Keying the animation LaunchedEffect directly on those booleans means
    // that clear can cancel the animation mid-flight, leaving it stuck partway. Instead, count
    // rising edges and key the animation on the counter so it always runs to completion once
    // started, regardless of what the flag does afterward.
    var spinTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(isCorrect) {
        if (isCorrect) spinTrigger++
    }
    LaunchedEffect(spinTrigger) {
        if (spinTrigger > 0) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, animationSpec = tween(450, easing = FastOutSlowInEasing))
            rotation.snapTo(0f)
        }
    }

    var shakeTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(isIncorrect) {
        if (isIncorrect) shakeTrigger++
    }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shakeX.snapTo(0f)
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -10f at 60
                    10f at 120
                    -8f at 180
                    8f at 240
                    -4f at 300
                    0f at 400
                },
            )
        }
    }

    BasicText(
        text = text,
        style = style,
        modifier = modifier.graphicsLayer(
            rotationZ = rotation.value,
            translationX = shakeX.value,
        ),
    )
}
