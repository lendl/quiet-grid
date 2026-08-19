// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuAnimalIcon.kt
package com.quietgrid.app.games.animaldoku

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

enum class AnimalDokuAnimalPose {
    PEEKING,
    FRONT_FACING,
}

/**
 * One woodland animal emoji per region, indexed the same way as [REGION_PALETTE] so a puzzle's
 * regions each get a consistent, distinct animal (region 0 is always the fox, region 1 always the
 * owl, and so on) matching their consistent distinct color.
 */
private val ANIMAL_EMOJI_PALETTE = listOf(
    "🦊", "🦉", "🦔", "🐻", "🐰", "🐿️", "🦡", "🦌", "🐺",
)

/**
 * A flat emoji-style animal marker, sized relative to its constrained box so it scales cleanly to
 * any cell size across AnimalDoku's 5x5-9x9 no-zoom range. PEEKING renders smaller and lower (the
 * pre-settle frame of the correct-open jump-and-settle animation); FRONT_FACING is the full
 * centered mark. [tint] is accepted for call-site compatibility but unused -- color emoji glyphs
 * render in their own fixed native colors, not a themeable tint.
 */
@Composable
fun AnimalDokuAnimalIcon(pose: AnimalDokuAnimalPose, region: Int, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val emoji = ANIMAL_EMOJI_PALETTE[region % ANIMAL_EMOJI_PALETTE.size]
    val scale = if (pose == AnimalDokuAnimalPose.PEEKING) 0.62f else 1f
    val alignment = if (pose == AnimalDokuAnimalPose.PEEKING) Alignment.BottomCenter else Alignment.Center
    BoxWithConstraints(modifier, contentAlignment = alignment) {
        BasicText(
            text = emoji,
            style = TextStyle(fontSize = (maxWidth.value * scale * 0.85f).sp, textAlign = TextAlign.Center),
        )
    }
}
