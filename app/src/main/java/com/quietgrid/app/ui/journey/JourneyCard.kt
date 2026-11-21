package com.quietgrid.app.ui.journey

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.pressScale
import com.quietgrid.app.ui.theme.celebrationGold

@Composable
fun JourneyCard(gameId: GameId, onOpen: (Int?) -> Unit) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsStateWithLifecycle(initialValue = null)
    val summary = state?.summary ?: return
    val chapterIndex = summary.chapterIndex
    val stopNumber = summary.stopNumber
    val position = if (chapterIndex != null && stopNumber != null) {
        stringResource(R.string.journey_card_position, chapterIndex, stopNumber)
    } else {
        stringResource(R.string.journey_complete)
    }
    JourneyCardSurface(onClick = { onOpen(null) }) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.journey_title), style = MaterialTheme.typography.titleMedium)
            Text(
                position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state?.mastered == true) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.journey_mastered_badge),
                        tint = celebrationGold().gold,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp),
                    )
                }
                Text(
                    stringResource(R.string.journey_card_stars, summary.stars, summary.maxStars),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Button(onClick = { onOpen(chapterIndex) }) { Text(stringResource(R.string.journey_continue)) }
    }
}

@Composable
fun JourneyCardSurface(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .heightIn(min = 88.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
