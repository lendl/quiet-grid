package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.JourneyStopState

@Composable
fun JourneyStopCard(
    row: JourneyStopRow,
    preview: JourneyBoardPreviewData?,
    onPlay: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val separator = " " + stringResource(R.string.play_info_separator) + " "
    val meta = listOfNotNull(
        when (preview) {
            is JourneyBoardPreviewData.Arrows -> stringResource(R.string.journey_stop_meta_arrows, preview.cols, preview.rows, preview.pieces.size)
            is JourneyBoardPreviewData.Regions -> stringResource(R.string.journey_stop_meta_grid, preview.size)
            is JourneyBoardPreviewData.Digits -> stringResource(R.string.journey_stop_meta_grid, preview.size)
            is JourneyBoardPreviewData.Cages -> stringResource(R.string.journey_stop_meta_grid, preview.size)
            is JourneyBoardPreviewData.EmptyGrid -> stringResource(R.string.journey_stop_meta_rect, preview.cols, preview.rows)
            null -> null
        },
        stringResource(R.string.journey_stop_target, formatElapsed(row.stop.targetSeconds)),
    ).joinToString(separator)
    val best = when {
        row.progress.cleared -> pluralStringResource(
            R.plurals.journey_stop_best,
            row.progress.stars,
            formatElapsed(row.progress.bestSeconds ?: 0),
            row.progress.stars,
        )
        row.progress.skipped -> stringResource(R.string.journey_stop_skipped_now)
        else -> stringResource(R.string.journey_stop_not_played)
    }
    Column(modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) JourneyBoardPreview(preview, Modifier.size(72.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.journey_stop, row.number),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                )
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(best, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalChip(stringResource(R.string.journey_goal_chip_clear), row.progress.cleared, Modifier.weight(1f))
            GoalChip(stringResource(R.string.journey_goal_chip_clean), row.progress.cleared && row.progress.clean, Modifier.weight(1f))
            GoalChip(formatElapsed(row.stop.targetSeconds), row.progress.cleared && row.progress.underTarget, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val buttonModifier = Modifier
                .weight(1f)
                .height(52.dp)
            when (row.state) {
                JourneyStopState.DONE -> FilledTonalButton(onClick = onPlay, modifier = buttonModifier) {
                    Text(stringResource(R.string.journey_replay))
                }
                else -> Button(onClick = onPlay, enabled = row.state != JourneyStopState.LATER, modifier = buttonModifier) {
                    Text(stringResource(R.string.journey_play))
                }
            }
            if (row.state == JourneyStopState.CURRENT || row.state == JourneyStopState.OPEN) {
                TextButton(onClick = onSkip, modifier = Modifier.height(52.dp)) { Text(stringResource(R.string.journey_skip)) }
            }
        }
    }
}

@Composable
private fun GoalChip(label: String, earned: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val starDescription = stringResource(if (earned) R.string.journey_star_earned else R.string.journey_star_missed)
    Column(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = starDescription,
            tint = if (earned) colors.primary else colors.outlineVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurface, maxLines = 1)
    }
}
