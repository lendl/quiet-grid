package com.quietgrid.app.games.wordswap

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.sqrt

data class WordSwapColors(
    val tile: Color,
    val onTile: Color,
    val selected: Color,
    val onSelected: Color,
    val done: Color,
    val onDone: Color,
    val hint: Color,
    val onHint: Color,
    val diagonal: Color,
)

private val PENCIL_WORDSWAP_COLORS = WordSwapColors(
    tile = Color(0xFFE4E4E4),
    onTile = Color(0xFF1F1F1F),
    selected = Color(0xFF8C8C8C),
    onSelected = Color(0xFFFFFFFF),
    done = Color(0xFFC2C2C2),
    onDone = Color(0xFF1F1F1F),
    hint = Color(0xFF5E5E5E),
    onHint = Color(0xFFFFFFFF),
    diagonal = Color(0xFFC8C8C8),
)

private const val STRONG_SEPARATION = 0.25f
private const val SELECTED_HINT_SEPARATION = 0.15f

private fun colorDistance(a: Color, b: Color): Float {
    val r = a.red - b.red
    val g = a.green - b.green
    val bl = a.blue - b.blue
    return sqrt(r * r + g * g + bl * bl)
}

fun wordSwapColorsFor(scheme: ColorScheme, pencil: Boolean): WordSwapColors {
    if (pencil) return PENCIL_WORDSWAP_COLORS
    val tile = scheme.surfaceContainerHighest
    val done = scheme.tertiaryContainer
    val selected = scheme.inverseSurface
    val candidates = listOf(
        scheme.primary to scheme.onPrimary,
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiary to scheme.onTertiary,
    )
    val hint = candidates.firstOrNull { (fill, _) ->
        colorDistance(fill, tile) >= STRONG_SEPARATION &&
            colorDistance(fill, done) >= STRONG_SEPARATION &&
            colorDistance(fill, selected) >= SELECTED_HINT_SEPARATION
    } ?: candidates.first()
    return WordSwapColors(
        tile = tile,
        onTile = scheme.onSurface,
        selected = selected,
        onSelected = scheme.inverseOnSurface,
        done = done,
        onDone = scheme.onTertiaryContainer,
        hint = hint.first,
        onHint = hint.second,
        diagonal = scheme.outlineVariant,
    )
}

@Composable
fun rememberWordSwapColors(): WordSwapColors = wordSwapColorsFor(MaterialTheme.colorScheme, LocalIsPencilTheme.current)
