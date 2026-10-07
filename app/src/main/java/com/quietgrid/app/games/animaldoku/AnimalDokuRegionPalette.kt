package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import com.quietgrid.engine.animaldoku.assignRegionColors

internal val ANIMALDOKU_REGION_PALETTE_LIGHT = listOf(
    Color(0xFF56B4E9), Color(0xFF0072B2), Color(0xFF009E73), Color(0xFFCC79A7),
    Color(0xFFE69F00), Color(0xFF882255), Color(0xFF9C7A00), Color(0xFF999999), Color(0xFFD55E00),
)

internal val ANIMALDOKU_REGION_PALETTE_DARK = listOf(
    Color(0xFFE69F00), Color(0xFF56B4E9), Color(0xFF009E73), Color(0xFFF0E442),
    Color(0xFF0072B2), Color(0xFFD55E00), Color(0xFFCC79A7), Color(0xFF999999), Color(0xFF882255),
)

internal val ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT: Set<Pair<Int, Int>> = setOf(
    6 to 8, 3 to 7, 2 to 7, 2 to 3, 4 to 8, 0 to 3, 4 to 6, 1 to 3, 2 to 5,
    0 to 1, 3 to 5,
    3 to 8, 1 to 2,
)

internal val ANIMALDOKU_FORBIDDEN_PAIRS_DARK: Set<Pair<Int, Int>> = setOf(
    6 to 7, 2 to 7, 2 to 6, 0 to 3, 0 to 5, 1 to 6, 4 to 6, 2 to 8, 1 to 4, 6 to 8, 7 to 8, 4 to 8, 1 to 7,
    5 to 6, 2 to 4,
)

private const val REGION_ALPHA_LIGHT = 0.85f
private const val REGION_ALPHA_DARK = 0.55f

@Composable
internal fun rememberAnimalDokuRegionColors(regions: List<List<Int>>): List<Color> {
    val isDarkTheme = LocalIsDarkTheme.current
    val palette = if (isDarkTheme) ANIMALDOKU_REGION_PALETTE_DARK else ANIMALDOKU_REGION_PALETTE_LIGHT
    val forbiddenPairs = if (isDarkTheme) ANIMALDOKU_FORBIDDEN_PAIRS_DARK else ANIMALDOKU_FORBIDDEN_PAIRS_LIGHT
    val alpha = if (isDarkTheme) REGION_ALPHA_DARK else REGION_ALPHA_LIGHT
    val assignment = remember(regions, forbiddenPairs) { assignRegionColors(regions, palette.size, forbiddenPairs) }
    return assignment.map { palette[it].copy(alpha = alpha) }
}
