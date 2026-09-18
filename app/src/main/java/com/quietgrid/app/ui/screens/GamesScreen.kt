package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameCategory
import com.quietgrid.app.core.GameId
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.GameMeta
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.components.AccountIconButton

@Composable
fun GamesScreen(
    onOpenGame: (GameId) -> Unit,
    onOpenAccount: () -> Unit,
) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())

    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }

    @Composable
    fun sortedBy(list: List<GameMeta>) = list
        .filter { selectedCategory == null || selectedCategory in it.categories }
        .map { it to stringResource(it.titleRes) }
        .sortedBy { it.second }
        .map { it.first }

    val readyGames = sortedBy(GameCatalog.games.filter { !it.beta })
    val betaGames = sortedBy(GameCatalog.games.filter { it.beta })

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AccountIconButton(onOpenAccount)
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.common_all)) },
                )
                GameCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(stringResource(category.labelRes)) },
                    )
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
            itemsIndexed(readyGames) { index, meta ->
                GameRow(meta, enabled = true, showDivider = index > 0, onClick = { onOpenGame(meta.id) })
            }

            if (betaGames.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.games_coming_soon),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = if (settings.betaGamesEnabled) 4.dp else 8.dp),
                    )
                    if (settings.betaGamesEnabled) {
                        Text(
                            stringResource(R.string.games_beta_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                items(betaGames) { meta ->
                    GameRow(meta, enabled = settings.betaGamesEnabled, onClick = { onOpenGame(meta.id) })
                }
            }
        }
    }
}

@Composable
private fun GameRow(meta: GameMeta, enabled: Boolean, showDivider: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        if (showDivider) HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(meta.titleRes), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(meta.taglineRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
