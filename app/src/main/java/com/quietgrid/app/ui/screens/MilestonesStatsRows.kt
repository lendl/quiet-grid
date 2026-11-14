package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.ui.components.BadgeDetailSheet
import com.quietgrid.app.ui.components.badgeStatusText
import com.quietgrid.app.ui.components.badgeText

private val GAME_BADGE_SIZE = 58.dp

@Composable
internal fun MilestonesStatsRow(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BadgeSummaryViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    if (!summary.show) return
    val count = summary.earned.size
    val newest = summary.earned.keys.lastOrNull()?.let { BadgeCatalog.byId(it) }
    val detail = if (newest != null) {
        pluralStringResource(R.plurals.milestones_stats_row_detail, count, count, stringResource(badgeText(newest).nameRes))
    } else {
        stringResource(R.string.milestones_stats_row_empty)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onOpen)
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(R.string.milestones_stats_row), style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun GameBadgesRow(
    gameId: GameId,
    modifier: Modifier = Modifier,
    viewModel: BadgeSummaryViewModel = hiltViewModel(),
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
