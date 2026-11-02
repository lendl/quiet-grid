package com.quietgrid.app.ui.journey

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.pressScale

@Composable
fun JourneyCard(gameId: GameId, onOpen: (Int?) -> Unit) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsState(initial = null)
    val summary = state?.summary ?: return
    val chapterIndex = summary.chapterIndex
    val stopNumber = summary.stopNumber
    val position = if (chapterIndex != null && stopNumber != null) {
        stringResource(R.string.journey_card_position, chapterIndex, stopNumber)
    } else {
        stringResource(R.string.journey_complete)
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = { onOpen(null) })
            .heightIn(min = 88.dp)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.journey_title), style = MaterialTheme.typography.titleMedium)
            Text(
                position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                stringResource(R.string.journey_card_stars, summary.stars, summary.maxStars),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Button(onClick = { onOpen(chapterIndex) }) { Text(stringResource(R.string.journey_continue)) }
    }
}
