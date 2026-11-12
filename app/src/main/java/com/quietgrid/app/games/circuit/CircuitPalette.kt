package com.quietgrid.app.games.circuit

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import kotlin.math.sqrt

data class CircuitPalette(
    val tile: Color,
    val lockedTile: Color,
    val hintTile: Color,
    val wire: Color,
    val litWire: Color,
    val lampFill: Color,
    val lampCore: Color,
    val frame: Color,
)

private val PENCIL_CIRCUIT_PALETTE = CircuitPalette(
    tile = Color(0xFFF0F0F0),
    lockedTile = Color(0xFFD0D0D0),
    hintTile = Color(0xFFB0B0B0),
    wire = Color(0xFF6E6E6E),
    litWire = Color(0xFF000000),
    lampFill = Color(0xFFFFFFFF),
    lampCore = Color(0xFFFFFFFF),
    frame = Color(0xFFC8C8C8),
)

private const val HINT_TILE_SEPARATION = 0.25f
private const val HINT_LOCKED_SEPARATION = 0.15f
private const val HINT_WIRE_SEPARATION = 0.3f

private fun colorDistance(a: Color, b: Color): Float {
    val r = a.red - b.red
    val g = a.green - b.green
    val bl = a.blue - b.blue
    return sqrt(r * r + g * g + bl * bl)
}

fun circuitPaletteFor(scheme: ColorScheme, pencil: Boolean): CircuitPalette {
    if (pencil) return PENCIL_CIRCUIT_PALETTE
    val tile = scheme.surfaceVariant
    val locked = scheme.secondaryContainer
    val candidates = listOf(scheme.tertiaryContainer, scheme.primaryContainer, scheme.inversePrimary, scheme.tertiary)
    val hintTile = candidates.firstOrNull { fill ->
        colorDistance(fill, tile) >= HINT_TILE_SEPARATION &&
            colorDistance(fill, locked) >= HINT_LOCKED_SEPARATION &&
            colorDistance(fill, scheme.primary) >= HINT_WIRE_SEPARATION
    } ?: candidates.first()
    return CircuitPalette(
        tile = tile,
        lockedTile = locked,
        hintTile = hintTile,
        wire = scheme.onSurfaceVariant.copy(alpha = 0.55f).compositeOver(tile),
        litWire = scheme.primary,
        lampFill = scheme.surface,
        lampCore = scheme.onPrimary,
        frame = scheme.outlineVariant,
    )
}

@Composable
fun rememberCircuitPalette(): CircuitPalette = circuitPaletteFor(MaterialTheme.colorScheme, LocalIsPencilTheme.current)
