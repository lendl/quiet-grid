package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.JourneyStopState

val JOURNEY_STOP_SIZE = 48.dp
val JOURNEY_CURRENT_STOP_SIZE = 60.dp

@Composable
fun JourneyStopHalo(modifier: Modifier = Modifier) {
    val glow = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(92.dp)) {
        drawCircle(glow.copy(alpha = 0.10f), radius = size.minDimension / 2)
        drawCircle(glow.copy(alpha = 0.16f), radius = size.minDimension / 2 * 0.8f)
    }
}

@Composable
fun JourneyStopNode(
    gameId: GameId,
    row: JourneyStopRow,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    val colors = MaterialTheme.colorScheme
    val tierLabel = stringResource(gameDifficultyLabelRes(gameId, Difficulty.fromKey(row.stop.difficulty)))
    val stateLabel = stringResource(
        when (row.state) {
            JourneyStopState.DONE -> R.string.journey_state_cleared
            JourneyStopState.SKIPPED -> R.string.journey_skipped
            JourneyStopState.CURRENT -> R.string.journey_up_next
            JourneyStopState.OPEN -> R.string.journey_state_open
            JourneyStopState.LATER -> R.string.journey_state_later
        },
    )
    val description = stringResource(R.string.journey_stop_description, row.number, tierLabel, stateLabel, row.progress.stars)
    val current = row.state == JourneyStopState.CURRENT
    val nodeSize: Dp = if (current) JOURNEY_CURRENT_STOP_SIZE else JOURNEY_STOP_SIZE
    val (fill, ink, edge) = when (row.state) {
        JourneyStopState.DONE -> Triple(colors.primaryContainer, colors.onPrimaryContainer, colors.primaryContainer)
        JourneyStopState.CURRENT -> Triple(colors.primary, colors.onPrimary, colors.primary)
        JourneyStopState.SKIPPED, JourneyStopState.OPEN -> Triple(colors.surface, colors.primary, colors.primary)
        JourneyStopState.LATER -> Triple(colors.surface, colors.onSurfaceVariant, colors.outlineVariant)
    }
    val dashed = row.state == JourneyStopState.SKIPPED
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(JOURNEY_CURRENT_STOP_SIZE + 8.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    Modifier
                        .size(nodeSize + 8.dp)
                        .border(2.dp, colors.primary, shape),
                )
            }
            Box(
                Modifier
                    .size(nodeSize)
                    .alpha(if (row.state == JourneyStopState.LATER) 0.6f else 1f)
                    .clip(shape)
                    .background(fill)
                    .drawBehind {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val width = 2.dp.toPx()
                        val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null
                        if (edge != fill || dashed) drawOutline(outline, edge, style = Stroke(width * 2, pathEffect = effect))
                    }
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                        this.selected = selected
                    }
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    row.number.toString(),
                    style = if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        StopStars(row.progress.stars.takeIf { row.state == JourneyStopState.DONE } ?: 0, colors.primary)
    }
}

@Composable
private fun StopStars(stars: Int, tint: Color) {
    Row(Modifier.height(12.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(stars) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        }
    }
}
