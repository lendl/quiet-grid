package com.quietgrid.app.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

val StarRevealScale = SemanticsPropertyKey<Float>("StarRevealScale")
var SemanticsPropertyReceiver.starRevealScale by StarRevealScale

private const val MIDDLE_SLOT = 1

@Composable
fun StarReveal(earned: Int, elapsedMs: Long, modifier: Modifier = Modifier) {
    val earnedDescription = stringResource(R.string.journey_star_earned)
    val missedDescription = stringResource(R.string.journey_star_missed)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
        repeat(JOURNEY_MAX_STARS_PER_STOP) { slot ->
            val revealIndex = STAR_REVEAL_SLOTS.indexOf(slot)
            val isEarned = revealIndex < earned
            val frame = if (isEarned) gentlePopFrame(elapsedMs, revealIndex) else StarFrame(1f, 1f)
            val size = if (slot == MIDDLE_SLOT) 84.dp else 64.dp
            Box(
                Modifier
                    .size(size)
                    .semantics {
                        contentDescription = if (isEarned) earnedDescription else missedDescription
                        starRevealScale = frame.scale
                    },
            ) {
                Icon(
                    Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isEarned) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = frame.scale
                                scaleY = frame.scale
                                alpha = frame.alpha
                            },
                    )
                }
            }
        }
    }
}
