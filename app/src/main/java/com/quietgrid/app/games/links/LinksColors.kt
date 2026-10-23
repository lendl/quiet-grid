package com.quietgrid.app.games.links

import androidx.compose.ui.graphics.Color

private val LINKS_COLORS = listOf(
    Color(0xFFE5484D),
    Color(0xFF3E8EF7),
    Color(0xFF30A46C),
    Color(0xFFF5C400),
    Color(0xFFF76B15),
    Color(0xFF12A5B8),
    Color(0xFFD6409F),
    Color(0xFF8E4EC6),
    Color(0xFFA07553),
    Color(0xFF8BC34A),
    Color(0xFF3F51B5),
    Color(0xFF00796B),
    Color(0xFF9E9E9E),
    Color(0xFF8B1E2D),
)

private val LINKS_PENCIL_COLORS = listOf(
    Color(0xFF1A1A1A),
    Color(0xFF6B6B6B),
    Color(0xFF3D3D3D),
    Color(0xFF8A8A8A),
    Color(0xFF2B2B2B),
    Color(0xFF5A5A5A),
    Color(0xFF7A7A7A),
    Color(0xFF474747),
)

fun linksPairColor(color: Int, pencil: Boolean): Color =
    if (pencil) LINKS_PENCIL_COLORS[color % LINKS_PENCIL_COLORS.size] else LINKS_COLORS[color % LINKS_COLORS.size]

fun linksPairLetter(color: Int): String = ('A' + color).toString()
