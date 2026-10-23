package com.quietgrid.app.games.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.assignLinksPairColors

private const val LETTER_DARK_LUMINANCE = 0.18f

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

private val LINKS_CONFUSABLE_PAIRS: Set<Pair<Int, Int>> = setOf(
    0 to 13, 0 to 4, 0 to 6, 1 to 10, 1 to 5, 7 to 10, 6 to 7, 2 to 9, 2 to 11, 5 to 11,
    3 to 9, 3 to 4, 4 to 8, 8 to 13, 8 to 12,
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

private val LINKS_PENCIL_CONFUSABLE_PAIRS: Set<Pair<Int, Int>> = setOf(
    0 to 4, 4 to 2, 2 to 7, 7 to 5, 5 to 1, 1 to 6, 6 to 3,
)

fun linksPaletteColor(slot: Int, pencil: Boolean): Color =
    if (pencil) LINKS_PENCIL_COLORS[slot % LINKS_PENCIL_COLORS.size] else LINKS_COLORS[slot % LINKS_COLORS.size]

@Composable
fun rememberLinksPairColors(puzzle: LinksPuzzleEntry): List<Color> {
    val pencil = LocalIsPencilTheme.current
    return remember(puzzle.id, pencil) {
        val palette = if (pencil) LINKS_PENCIL_COLORS else LINKS_COLORS
        val confusable = if (pencil) LINKS_PENCIL_CONFUSABLE_PAIRS else LINKS_CONFUSABLE_PAIRS
        assignLinksPairColors(puzzle.size, puzzle.solution, palette.size, confusable).map { palette[it] }
    }
}

fun linksLetterColor(pairColor: Color): Color =
    if (pairColor.luminance() > LETTER_DARK_LUMINANCE) Color.Black else Color.White

fun linksPairLetter(color: Int): String = ('A' + color).toString()
