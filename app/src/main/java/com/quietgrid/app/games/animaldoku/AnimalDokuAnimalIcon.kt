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

private val ANIMAL_EMOJI_PALETTE = listOf(
    "🦊", "🦉", "🦔", "🐻", "🐰", "🐿️", "🦡", "🦌", "🐺",
)

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
