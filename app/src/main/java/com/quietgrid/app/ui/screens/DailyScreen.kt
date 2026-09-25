package com.quietgrid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.DailyGameUi
import com.quietgrid.app.core.daily.DailyTierStatus
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.notifications.rememberFirstSubscribePermissionRequest
import com.quietgrid.app.ui.components.AccountIconButton
import com.quietgrid.app.ui.components.StatGroup
import com.quietgrid.app.ui.components.StatItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private data class TodayEntry(val gameId: GameId, val difficulty: Difficulty, val inProgress: Boolean)

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
    val requestNotificationPermission = rememberFirstSubscribePermissionRequest()
    var openGameKey by rememberSaveable { mutableStateOf<String?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val dateText = remember(state.today, locale) {
        state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }

    val todayEntries = state.games.flatMap { game ->
        game.tiers.mapNotNull { tier ->
            when (tier.status) {
                DailyTierStatus.Unplayed -> TodayEntry(game.gameId, tier.difficulty, inProgress = false)
                DailyTierStatus.InProgress -> TodayEntry(game.gameId, tier.difficulty, inProgress = true)
                else -> null
            }
        }
    }

    val playTier: (GameId, Difficulty, DailyTierStatus) -> Unit = { gameId, difficulty, status ->
        when (status) {
            DailyTierStatus.Unplayed -> onStartDaily(gameId, difficulty, state.today)
            DailyTierStatus.InProgress -> onResumeDaily(gameId)
            is DailyTierStatus.Solved, DailyTierStatus.Lost ->
                scope.launch { viewModel.share(context, gameId, difficulty, state.today) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AccountIconButton(onOpenAccount)
            Text(
                stringResource(R.string.daily_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val todayTotal = state.games.sumOf { it.tiers.size }
        val todayDone = state.games.sumOf { game ->
            game.tiers.count { it.status is DailyTierStatus.Solved || it.status == DailyTierStatus.Lost }
        }

        LazyColumn(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "stats") {
                StatGroup(
                    listOf(
                        StatItem(
                            stringResource(R.string.stats_streak),
                            state.overallStreak.toString(),
                            stringResource(R.string.daily_stat_streak_desc),
                        ),
                        StatItem(
                            stringResource(R.string.daily_section_today),
                            "$todayDone/$todayTotal",
                            stringResource(R.string.daily_stat_today_desc),
                        ),
                        StatItem(
                            stringResource(R.string.stats_solved),
                            state.dailySolvedTotal.toString(),
                            stringResource(R.string.daily_stat_solved_desc),
                        ),
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "today_header") { SectionHeader(stringResource(R.string.daily_section_lineup)) }
            when {
                todayEntries.isNotEmpty() -> items(todayEntries, key = { "today_${it.gameId.key}_${it.difficulty.key}" }) { entry ->
                    TodayRow(
                        entry = entry,
                        onPlay = {
                            playTier(
                                entry.gameId,
                                entry.difficulty,
                                if (entry.inProgress) DailyTierStatus.InProgress else DailyTierStatus.Unplayed,
                            )
                        },
                    )
                }
                state.games.isNotEmpty() -> item(key = "today_done") { SectionHint(stringResource(R.string.daily_all_done)) }
                else -> item(key = "today_empty") { SectionHint(stringResource(R.string.daily_today_empty)) }
            }

            item(key = "games_header") {
                Column {
                    SectionHeader(stringResource(R.string.daily_section_games))
                    SectionHint(stringResource(R.string.daily_section_games_hint))
                }
            }
            items(state.available.keys.toList(), key = { "game_${it.key}" }) { gameId ->
                DailyGameRow(
                    gameId = gameId,
                    subscribedTiers = state.available[gameId].orEmpty().filter { it in state.subscribed[gameId].orEmpty() },
                    game = state.games.firstOrNull { it.gameId == gameId },
                    onClick = { openGameKey = gameId.key },
                )
            }
        }
    }

    val openGameId = state.available.keys.firstOrNull { it.key == openGameKey }
    if (openGameId != null) {
        ModalBottomSheet(onDismissRequest = { openGameKey = null }) {
            DailyGameSheet(
                gameId = openGameId,
                availableTiers = state.available[openGameId].orEmpty(),
                subscribedTiers = state.subscribed[openGameId].orEmpty(),
                game = state.games.firstOrNull { it.gameId == openGameId },
                onToggleTier = { difficulty, subscribe ->
                    viewModel.setSubscribed(openGameId, difficulty, subscribe)
                    if (subscribe) requestNotificationPermission()
                },
                onTierAction = { difficulty, status ->
                    if (status == DailyTierStatus.Unplayed || status == DailyTierStatus.InProgress) openGameKey = null
                    playTier(openGameId, difficulty, status)
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TodayRow(entry: TodayEntry, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(GameCatalog.get(entry.gameId).titleRes), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(gameDifficultyLabelRes(entry.gameId, entry.difficulty)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onPlay) {
                Text(stringResource(if (entry.inProgress) R.string.daily_resume else R.string.common_play))
            }
        }
    }
}

@Composable
private fun DailyGameRow(
    gameId: GameId,
    subscribedTiers: List<Difficulty>,
    game: DailyGameUi?,
    onClick: () -> Unit,
) {
    val summary = if (subscribedTiers.isEmpty() || game == null) {
        stringResource(R.string.daily_not_subscribed)
    } else {
        val labels = subscribedTiers.map { stringResource(gameDifficultyLabelRes(gameId, it)) }.joinToString(", ")
        val done = game.tiers.count { it.status is DailyTierStatus.Solved || it.status == DailyTierStatus.Lost }
        "$labels · ${stringResource(R.string.daily_progress, done, game.tiers.size)}"
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(GameCatalog.get(gameId).titleRes), style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (game != null && game.streak > 0) {
                Text(
                    stringResource(R.string.daily_streak, game.streak),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun DailyGameSheet(
    gameId: GameId,
    availableTiers: List<Difficulty>,
    subscribedTiers: Set<Difficulty>,
    game: DailyGameUi?,
    onToggleTier: (Difficulty, Boolean) -> Unit,
    onTierAction: (Difficulty, DailyTierStatus) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(stringResource(GameCatalog.get(gameId).titleRes), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.daily_subscribe_tiers),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        availableTiers.forEachIndexed { index, difficulty ->
            if (index > 0) HorizontalDivider()
            val subscribed = difficulty in subscribedTiers
            val status = game?.tiers?.firstOrNull { it.difficulty == difficulty }?.status
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(gameDifficultyLabelRes(gameId, difficulty)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (subscribed && status != null) TierAction(status) { onTierAction(difficulty, status) }
                Switch(checked = subscribed, onCheckedChange = { onToggleTier(difficulty, it) })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TierAction(status: DailyTierStatus, onClick: () -> Unit) {
    when (status) {
        DailyTierStatus.Unplayed -> FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.common_play)) }
        DailyTierStatus.InProgress -> FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.daily_resume)) }
        is DailyTierStatus.Solved, DailyTierStatus.Lost -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (status is DailyTierStatus.Solved) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                if (status is DailyTierStatus.Solved) formatElapsed(status.elapsedSeconds) else stringResource(R.string.logs_result_lost),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.daily_share))
            }
        }
    }
}
