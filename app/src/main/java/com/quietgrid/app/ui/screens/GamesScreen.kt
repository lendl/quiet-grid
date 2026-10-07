package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameCategory
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.GameMeta
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.components.AccountIconButton
import com.quietgrid.app.ui.components.GameIcon

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
    val betaGamesEnabled = settings.betaGamesEnabled

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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val layout = gamesGridLayout(maxWidth)
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.columns),
                contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(layout.spacing),
                verticalArrangement = Arrangement.spacedBy(layout.spacing + 4.dp),
            ) {
                items(readyGames, key = { it.id.key }) { meta ->
                    GameTile(meta, layout, enabled = true, onClick = { onOpenGame(meta.id) })
                }

                if (betaGames.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                stringResource(R.string.games_coming_soon),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, bottom = if (betaGamesEnabled) 4.dp else 0.dp),
                            )
                            if (betaGamesEnabled) {
                                Text(
                                    stringResource(R.string.games_beta_disclaimer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(betaGames, key = { it.id.key }) { meta ->
                        GameTile(meta, layout, enabled = betaGamesEnabled, onClick = { onOpenGame(meta.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTile(meta: GameMeta, layout: GamesGridLayout, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GameIcon(meta.id, size = min(maxWidth, layout.maxIconSize))
        }
        Text(
            stringResource(meta.titleRes),
            style = (if (layout.largeLabels) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium)
                .copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
