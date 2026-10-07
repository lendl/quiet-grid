package com.quietgrid.app.games.minesweeper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal data class MinesweeperPalette(
    val hidden: Color,
    val opened: Color,
    val numbers: List<Color>,
)

internal val MINESWEEPER_LIGHT_PALETTE = MinesweeperPalette(
    hidden = Color(0xFFE3D6FF),
    opened = Color(0xFFF1F2F5),
    numbers = listOf(
        Color(0xFF7131E3), Color(0xFF0E7C86), Color(0xFFB45309), Color(0xFFBE185D),
        Color(0xFF1D4ED8), Color(0xFF4D7C0F), Color(0xFF475569), Color(0xFF111827),
    ),
)

internal val MINESWEEPER_DARK_PALETTE = MinesweeperPalette(
    hidden = Color(0xFF3B3180),
    opened = Color(0xFF0F1318),
    numbers = listOf(
        Color(0xFFDAB9FF), Color(0xFF5EEAD4), Color(0xFFFBBF24), Color(0xFFF9A8D4),
        Color(0xFF93C5FD), Color(0xFFBEF264), Color(0xFFCBD5E1), Color(0xFFF8FAFC),
    ),
)

internal fun minesweeperPaletteFor(surface: Color): MinesweeperPalette =
    if (surface.luminance() < 0.5f) MINESWEEPER_DARK_PALETTE else MINESWEEPER_LIGHT_PALETTE
