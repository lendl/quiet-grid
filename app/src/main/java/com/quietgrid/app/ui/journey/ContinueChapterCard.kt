package com.quietgrid.app.ui.journey

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.GameIcon

@Composable
fun rememberActiveJourney(enabledGameIds: Set<GameId>): ActiveJourney? {
    val viewModel = journeyViewModel()
    val active by remember(viewModel, enabledGameIds) { viewModel.activeJourney(enabledGameIds) }
        .collectAsStateWithLifecycle(initialValue = null)
    return active
}

@Composable
fun ContinueChapterCard(journey: ActiveJourney, onContinue: (GameId, Int) -> Unit, modifier: Modifier = Modifier) {
    val open = { onContinue(journey.gameId, journey.chapterIndex) }
    JourneyCardSurface(onClick = open, modifier = modifier) {
        GameIcon(journey.gameId, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.journey_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(GameCatalog.get(journey.gameId).titleRes),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.journey_card_position, journey.chapterIndex, journey.stopNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = open) { Text(stringResource(R.string.journey_continue)) }
    }
}
