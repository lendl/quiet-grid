package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
    val loadedSettings by repositories.settingsRepository.settings.collectAsState(initial = null)
    val settings = loadedSettings ?: AppSettings()

    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }

    val titles = GameCatalog.games.associate { it.id to stringResource(it.titleRes) }

    fun sortedBy(list: List<GameMeta>) = gamesInGridOrder(
        list.filter { selectedCategory == null || selectedCategory in it.categories },
        titles,
        settings.favoriteGameIds,
    )

    val readyGames = if (loadedSettings == null) emptyList() else sortedBy(GameCatalog.games.filter { !it.beta })
    val betaGames = if (loadedSettings == null) emptyList() else sortedBy(GameCatalog.games.filter { it.beta })
    val betaGamesEnabled = settings.betaGamesEnabled
    val isFavorite = { meta: GameMeta -> meta.id.key in settings.favoriteGameIds }
    val isNew = { meta: GameMeta -> meta.isNew && meta.id.key !in settings.newBadgeSeenGameIds }

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
                    GameTile(meta, layout, enabled = true, isFavorite = isFavorite(meta), isNew = isNew(meta), onClick = { onOpenGame(meta.id) })
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
                        GameTile(meta, layout, enabled = betaGamesEnabled, isFavorite = isFavorite(meta), isNew = isNew(meta), onClick = { onOpenGame(meta.id) })
                    }
                }
            }
        }
    }
}

internal fun gamesInGridOrder(games: List<GameMeta>, titles: Map<GameId, String>, favoriteGameIds: Set<String>): List<GameMeta> =
    games.sortedWith(compareByDescending<GameMeta> { it.id.key in favoriteGameIds }.thenBy { titles[it.id].orEmpty() })

private val BADGE_RING = 2.dp
private val NEW_BADGE_OVERHANG_X = 10.dp
private val NEW_BADGE_OVERHANG_Y = 7.dp
private val FAVORITE_BADGE_SIZE = 28.dp
private val FAVORITE_BADGE_OVERHANG = 7.dp

@Composable
private fun GameTile(meta: GameMeta, layout: GamesGridLayout, enabled: Boolean, isFavorite: Boolean, isNew: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 4.dp + NEW_BADGE_OVERHANG_Y, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val iconSize = min(maxWidth - NEW_BADGE_OVERHANG_X * 2, layout.maxIconSize)
            Box {
                GameIcon(meta.id, size = iconSize)
                if (isNew) {
                    NewBadge(Modifier.align(Alignment.TopEnd).offset(x = NEW_BADGE_OVERHANG_X, y = -NEW_BADGE_OVERHANG_Y))
                }
                if (isFavorite) {
                    FavoriteBadge(Modifier.align(Alignment.BottomStart).offset(x = -FAVORITE_BADGE_OVERHANG, y = FAVORITE_BADGE_OVERHANG))
                }
            }
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

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.games_badge_new),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(BADGE_RING)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

@Composable
private fun FavoriteBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(FAVORITE_BADGE_SIZE)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(BADGE_RING)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = stringResource(R.string.games_favorite),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}
