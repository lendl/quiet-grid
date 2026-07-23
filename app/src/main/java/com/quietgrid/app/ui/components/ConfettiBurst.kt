package com.quietgrid.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Int,
    val sizeDp: Float,
    val rotationSpeed: Float,
)

/** One-shot confetti burst falling from the top of its bounds. Plays once and stops. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, particleCount: Int = 40) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )
    val particles = remember {
        val random = Random(System.nanoTime())
        List(particleCount) {
            ConfettiParticle(
                startXFraction = random.nextFloat(),
                velocityX = (random.nextFloat() - 0.5f) * 0.6f,
                velocityY = 0.5f + random.nextFloat() * 0.5f,
                color = random.nextInt(colors.size),
                sizeDp = 5f + random.nextFloat() * 5f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 720f,
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = LinearEasing))
    }

    if (progress.value >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        val alpha = min(1f, (1f - t) * 2.2f).coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas

        particles.forEach { particle ->
            val x = (particle.startXFraction + particle.velocityX * t) * size.width
            val y = (particle.velocityY * t + 0.6f * t * t) * size.height
            if (y > size.height) return@forEach

            val particleSizePx = particle.sizeDp * density
            rotate(degrees = particle.rotationSpeed * t, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[particle.color].copy(alpha = alpha),
                    topLeft = Offset(x - particleSizePx / 2, y - particleSizePx / 2),
                    size = androidx.compose.ui.geometry.Size(particleSizePx, particleSizePx),
                )
            }
        }
    }
}
