package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.appViewModel
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.ui.components.BadgeDetailSheet
import com.quietgrid.app.ui.components.badgeStatusText

private val GAME_BADGE_SIZE = 58.dp
private val SHELF_BADGE_SIZE = 50.dp
private const val SHELF_COUNT = 4

@Composable
internal fun MilestonesStatsRow(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BadgeSummaryViewModel = appViewModel { graph -> BadgeSummaryViewModel(graph.badgeRepository, graph.settingsRepository) },
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!summary.show) return
    val earnedCount = BadgeCatalog.all.count { it.id in summary.earned }
    val newest = summary.earned.keys.toList().takeLast(SHELF_COUNT).reversed().mapNotNull { BadgeCatalog.byId(it) }
    val title = stringResource(R.string.stats_badges_title)
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(top = 20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 10.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .alignByBaseline()
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.alignByBaseline())
                Text(
                    stringResource(R.string.stats_badges_count, earnedCount, BadgeCatalog.all.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .alignByBaseline(),
                )
            }
            Box(
                Modifier
                    .alignByBaseline()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = title, onClick = onOpen)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.stats_badges_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (newest.isEmpty()) {
            Text(
                stringResource(R.string.milestones_stats_row_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                newest.forEach { def ->
                    val earnedOn = summary.earned[def.id]
                    BadgeTile(
                        def = def,
                        earned = earnedOn != null,
                        badgeSize = SHELF_BADGE_SIZE,
                        status = badgeStatusText(earnedOn, null),
                        onClick = { openId = def.id },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(SHELF_COUNT - newest.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    openId?.let { id -> BadgeCatalog.byId(id) }?.let { def ->
        BadgeDetailSheet(def = def, earnedOn = summary.earned[def.id], count = null, onDismiss = { openId = null })
    }
}

@Composable
internal fun GameBadgesRow(
    gameId: GameId,
    modifier: Modifier = Modifier,
    viewModel: BadgeSummaryViewModel = appViewModel { graph -> BadgeSummaryViewModel(graph.badgeRepository, graph.settingsRepository) },
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val defs = remember(gameId) { BadgeCatalog.forGame(gameId) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!summary.show || defs.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.stats_badges_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            defs.forEach { def ->
                val earnedOn = summary.earned[def.id]
                BadgeTile(
                    def = def,
                    earned = earnedOn != null,
                    badgeSize = GAME_BADGE_SIZE,
                    status = badgeStatusText(earnedOn, null),
                    onClick = { openId = def.id },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    openId?.let { id -> defs.firstOrNull { it.id == id } }?.let { def ->
        BadgeDetailSheet(def = def, earnedOn = summary.earned[def.id], count = null, onDismiss = { openId = null })
    }
}
