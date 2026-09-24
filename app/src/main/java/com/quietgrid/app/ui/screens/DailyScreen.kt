package com.quietgrid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.DailyTierStatus
import com.quietgrid.app.ui.components.AccountIconButton
import com.quietgrid.app.ui.components.DailyTierChips
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    onOpenAccount: () -> Unit,
    onStartDaily: (GameId, Difficulty, LocalDate) -> Unit,
    onResumeDaily: (GameId) -> Unit,
) {
    val viewModel: DailyViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSubscriptions by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AccountIconButton(onOpenAccount)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showSubscriptions = true }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.daily_manage))
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (state.overallStreak > 0) {
                Text(stringResource(R.string.daily_streak, state.overallStreak), style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(state.today.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (state.subscribed.none { it in state.eligible }) {
            Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.daily_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.daily_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = { showSubscriptions = true }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.daily_empty_action))
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.games, key = { it.gameId.key }) { game ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(GameCatalog.get(game.gameId).titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (game.streak > 0) {
                                    Text(stringResource(R.string.daily_streak, game.streak), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Spacer(Modifier.padding(top = 8.dp))
                            DailyTierChips(game.gameId, game.tiers) { tier ->
                                when (tier.status) {
                                    DailyTierStatus.Unplayed -> onStartDaily(game.gameId, tier.difficulty, state.today)
                                    DailyTierStatus.InProgress -> onResumeDaily(game.gameId)
                                    is DailyTierStatus.Solved, DailyTierStatus.Lost ->
                                        scope.launch { viewModel.share(context, game.gameId, tier.difficulty, state.today) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubscriptions) {
        ModalBottomSheet(onDismissRequest = { showSubscriptions = false }) {
            Text(
                stringResource(R.string.daily_subscriptions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            state.eligible.forEach { gameId ->
                val subscribed = gameId in state.subscribed
                ListItem(
                    headlineContent = { Text(stringResource(GameCatalog.get(gameId).titleRes)) },
                    trailingContent = {
                        Switch(checked = subscribed, onCheckedChange = { viewModel.setSubscribed(gameId, it) })
                    },
                )
            }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
