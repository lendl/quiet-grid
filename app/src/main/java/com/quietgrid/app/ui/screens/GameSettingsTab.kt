package com.quietgrid.app.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.themes.ThemeCount
import com.quietgrid.app.core.themes.sparseTiers
import com.quietgrid.app.core.themes.themeIcon
import com.quietgrid.app.core.themes.themeLabelRes
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.games.themeclear.ThemeClearPuzzleBank
import com.quietgrid.app.games.themeclear.currentThemeClearLocale
import com.quietgrid.app.games.wordsearch.WordSearchPuzzleBank
import com.quietgrid.app.games.wordsearch.currentWordSearchLocale
import kotlinx.coroutines.launch

private val THEMED_GAMES = setOf(GameId.WORDSEARCH, GameId.THEMECLEAR)

private val COUNT_COLUMN_WIDTH = 38.dp

private val TIER_COLUMN_LABELS = listOf(
    Difficulty.EASY to R.string.game_settings_themes_col_easy,
    Difficulty.MEDIUM to R.string.game_settings_themes_col_medium,
    Difficulty.HARD to R.string.game_settings_themes_col_hard,
    Difficulty.EXPERT to R.string.game_settings_themes_col_expert,
)

fun gameHasSettings(gameId: GameId): Boolean = gameId in THEMED_GAMES

private fun themeLocaleFor(gameId: GameId, puzzleLanguage: String): String = when (gameId) {
    GameId.THEMECLEAR -> currentThemeClearLocale(puzzleLanguage)
    else -> currentWordSearchLocale(puzzleLanguage)
}

private suspend fun themeCountsFor(gameId: GameId, context: Context, locale: String): List<ThemeCount> = when (gameId) {
    GameId.THEMECLEAR -> ThemeClearPuzzleBank.themeCounts(context, locale)
    GameId.WORDSEARCH -> WordSearchPuzzleBank.themeCounts(context, locale)
    else -> emptyList()
}

private fun themeLabel(context: Context, themeId: String): String = context.getString(themeLabelRes(themeId))

@Composable
fun GameSettingsTab(gameId: GameId) {
    if (gameId in THEMED_GAMES) ThemeSelectionSection(gameId)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeSelectionSection(gameId: GameId) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = null)
    val puzzleLanguage = settings?.puzzleLanguage ?: return
    val locale = themeLocaleFor(gameId, puzzleLanguage)
    val counts by produceState<List<ThemeCount>?>(null, gameId, locale) {
        value = themeCountsFor(gameId, context, locale)
    }
    val excludedFlow = remember(gameId, locale) { repositories.themePreferencesRepository.excludedThemes(gameId, locale) }
    val excluded by excludedFlow.collectAsState(initial = null)
    var openThemeId by rememberSaveable(gameId) { mutableStateOf<String?>(null) }
    val currentCounts = counts ?: return
    val currentExcluded = excluded ?: return

    val labels = remember(currentCounts, gameId) {
        currentCounts.associate { it.themeId to themeLabel(context, it.themeId) }
    }
    val sorted = remember(currentCounts, labels) { currentCounts.sortedBy { labels.getValue(it.themeId).lowercase() } }
    val allIds = currentCounts.map { it.themeId }.toSet()
    val effectiveExcluded = currentExcluded intersect allIds
    val selectedCount = allIds.size - effectiveExcluded.size
    val selected = currentCounts.filter { it.themeId !in effectiveExcluded }
    val selectedTotals = Difficulty.entries.associateWith { tier -> selected.sumOf { it.perTier[tier] ?: 0 } }
    val sparse = sparseTiers(currentCounts, currentExcluded).map { it.difficulty }.toSet()
    val headerStyle = MaterialTheme.typography.labelSmall
    val headerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val totalsStyle = MaterialTheme.typography.labelLarge
    val totalsColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item {
            Text(stringResource(R.string.game_settings_themes_title), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (effectiveExcluded.isEmpty()) {
                        stringResource(R.string.game_settings_themes_all)
                    } else {
                        stringResource(R.string.game_settings_themes_selected, selectedCount, allIds.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch { repositories.themePreferencesRepository.setExcludedThemes(gameId, locale, emptySet()) }
                    },
                    enabled = effectiveExcluded.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.game_settings_themes_select_all))
                }
            }
            Text(
                stringResource(R.string.game_settings_themes_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.game_settings_themes_words_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("", modifier = Modifier.weight(1f))
                        CountCell(stringResource(R.string.game_settings_themes_col_total), headerStyle, headerColor)
                        TIER_COLUMN_LABELS.forEach { (_, labelRes) ->
                            CountCell(stringResource(labelRes), headerStyle, headerColor)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.game_settings_themes_selected_row),
                            style = totalsStyle,
                            modifier = Modifier.weight(1f),
                        )
                        CountCell(selectedTotals.values.sum().toString(), totalsStyle, totalsColor)
                        TIER_COLUMN_LABELS.forEach { (tier, _) ->
                            val isSparse = tier in sparse
                            CountCell(
                                selectedTotals.getValue(tier).toString(),
                                if (isSparse) totalsStyle.copy(fontWeight = FontWeight.Bold) else totalsStyle,
                                if (isSparse) MaterialTheme.colorScheme.error else totalsColor,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        items(sorted, key = { it.themeId }) { count ->
            val checked = count.themeId !in effectiveExcluded
            val canToggle = !checked || selectedCount > 1
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { openThemeId = count.themeId },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        scope.launch {
                            repositories.themePreferencesRepository.toggleExcludedTheme(gameId, locale, count.themeId, allIds)
                        }
                    },
                    enabled = canToggle,
                )
                Row(
                    Modifier.weight(1f).alpha(if (checked) 1f else 0.5f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    themeIcon(count.themeId)?.let { Text(it, modifier = Modifier.padding(start = 8.dp)) }
                    Text(
                        labels.getValue(count.themeId),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp, end = 4.dp),
                    )
                    CountCell(count.total.toString(), MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurface)
                    TIER_COLUMN_LABELS.forEach { (tier, _) ->
                        CountCell(
                            (count.perTier[tier] ?: 0).toString(),
                            MaterialTheme.typography.bodySmall,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }

    openThemeId?.let { themeId ->
        ThemeWordsSheet(gameId = gameId, themeId = themeId, locale = locale, onDismiss = { openThemeId = null })
    }
}

@Composable
private fun RowScope.CountCell(text: String, style: TextStyle, color: Color) {
    Text(
        text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.width(COUNT_COLUMN_WIDTH),
    )
}
