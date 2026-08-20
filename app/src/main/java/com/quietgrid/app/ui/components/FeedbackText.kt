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
