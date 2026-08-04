package com.quietgrid.app.games.wordguess

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.wordguess.LetterState

/** Returns (background, foreground) for a letter tile/key, contrast-checked against both colors it can appear on. */
@Composable
fun wordGuessLetterColors(state: LetterState?): Pair<Color, Color> {
    val pencil = LocalIsPencilTheme.current
    return when (state) {
        null -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        LetterState.ABSENT -> if (pencil) {
            Color(0xFF616161) to Color.White
        } else {
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
        LetterState.PRESENT -> if (pencil) {
            Color(0xFF9E9E9E) to Color.Black
        } else {
            Color(0xFF8A6D00) to Color.White
        }
        LetterState.CORRECT -> if (pencil) {
            Color(0xFF212121) to Color.White
        } else {
            Color(0xFF1F7A3D) to Color.White
        }
    }
}
