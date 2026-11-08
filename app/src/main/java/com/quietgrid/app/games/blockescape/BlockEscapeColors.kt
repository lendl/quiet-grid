package com.quietgrid.app.games.blockescape

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

data class BlockEscapeColors(
    val key: Color,
    val block: Color,
    val activeBlock: Color,
    val hint: Color,
)

@Composable
fun rememberBlockEscapeColors(): BlockEscapeColors {
    val pencil = LocalIsPencilTheme.current
    val dark = LocalIsDarkTheme.current
    val hint = MaterialTheme.colorScheme.primary
    return when {
        pencil && dark -> BlockEscapeColors(Color(0xFFE6E6E6), Color(0xFF6E6E6E), Color(0xFF8E8E8E), Color(0xFFB8B8B8))
        pencil -> BlockEscapeColors(Color(0xFF2E2E2E), Color(0xFFB4B4B4), Color(0xFF959595), Color(0xFF5C5C5C))
        dark -> BlockEscapeColors(Color(0xFFE08A70), Color(0xFF5E7383), Color(0xFF7690A3), hint)
        else -> BlockEscapeColors(Color(0xFFD6785F), Color(0xFF97A9B7), Color(0xFF6C8CA2), hint)
    }
}
