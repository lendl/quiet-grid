package com.quietgrid.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val DARK_BACKGROUND_LUMINANCE = 0.5f

data class CelebrationGold(val gold: Color, val onGold: Color)

private val DarkBackgroundGold = CelebrationGold(gold = Color(0xFFE8C46A), onGold = Color(0xFF3A2A00))
private val LightBackgroundGold = CelebrationGold(gold = Color(0xFFB7860B), onGold = Color(0xFF2A1E00))

fun celebrationGoldFor(isPencil: Boolean, background: Color, onBackground: Color): CelebrationGold = when {
    isPencil -> CelebrationGold(gold = onBackground, onGold = background)
    background.luminance() < DARK_BACKGROUND_LUMINANCE -> DarkBackgroundGold
    else -> LightBackgroundGold
}

@Composable
fun celebrationGold(): CelebrationGold = celebrationGoldFor(
    isPencil = LocalIsPencilTheme.current,
    background = MaterialTheme.colorScheme.background,
    onBackground = MaterialTheme.colorScheme.onBackground,
)
