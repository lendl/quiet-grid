package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.core.badges.BadgeDef
import com.quietgrid.app.core.badges.BadgeSection
import com.quietgrid.app.core.badges.badgeCount
import com.quietgrid.app.ui.components.BadgeDetailSheet
import com.quietgrid.app.ui.components.BadgeHex
import com.quietgrid.app.ui.components.BadgeProgressBar
import com.quietgrid.app.ui.components.badgeAccent
import com.quietgrid.app.ui.components.badgeStatusText
import com.quietgrid.app.ui.components.badgeText

private val MILESTONE_SECTIONS: List<Pair<BadgeSection, Int>> = listOf(
    BadgeSection.ACROSS to R.string.milestones_group_across,
    BadgeSection.JOURNEYS to R.string.milestones_group_journeys,
    BadgeSection.PATIENCE to R.string.milestones_group_patience,
    BadgeSection.SEASONAL to R.string.milestones_group_seasonal,
)

private const val GRID_COLUMNS = 3
private const val RECENT_COUNT = 3
private val CELL_BADGE_SIZE = 64.dp
private val RECENT_BADGE_SIZE = 36.dp

@Composable
fun MilestonesScreen(modifier: Modifier = Modifier, viewModel: MilestonesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!state.loaded) return

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.milestones_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MilestonesSummaryCard(state, onOpen = { openId = it })
            MILESTONE_SECTIONS.forEach { (section, titleRes) ->
                MilestonesSection(stringResource(titleRes), BadgeCatalog.section(section), state, onOpen = { openId = it })
            }
            GameBadgesCard(state)
            Text(
                stringResource(R.string.milestones_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
    }

    openId?.let { id -> BadgeCatalog.byId(id) }?.let { def ->
        BadgeDetailSheet(
            def = def,
            earnedOn = state.earned[def.id],
            count = badgeCount(def.id, state.progress, state.journeys),
            dots = state.journeyDots(def),
            onDismiss = { openId = null },
        )
    }
}

@Composable
private fun MilestonesSummaryCard(state: MilestonesState, onOpen: (String) -> Unit) {
    val general = BadgeCatalog.general
    val earnedGeneral = general.count { it.id in state.earned }
    val recent = state.earned.keys.toList().takeLast(RECENT_COUNT).reversed().mapNotNull { BadgeCatalog.byId(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.milestones_earned_count, earnedGeneral, general.size),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                stringResource(R.string.milestones_earned_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (recent.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.milestones_recent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recent.forEach { def ->
                        val name = stringResource(badgeText(def).nameRes)
                        BadgeHex(
                            def = def,
                            earned = true,
                            size = RECENT_BADGE_SIZE,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClickLabel = name) { onOpen(def.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonesSection(title: String, defs: List<BadgeDef>, state: MilestonesState, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        defs.chunked(GRID_COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { def ->
                    MilestoneCell(def, state, onOpen, Modifier.weight(1f))
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MilestoneCell(def: BadgeDef, state: MilestonesState, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val earnedOn = state.earned[def.id]
    val count = badgeCount(def.id, state.progress, state.journeys)
    val status = badgeStatusText(earnedOn, count)
    BadgeTile(
        def = def,
        earned = earnedOn != null,
        badgeSize = CELL_BADGE_SIZE,
        isNew = def.id == state.newBadgeId,
        status = status,
        onClick = { onOpen(def.id) },
        modifier = modifier,
        dots = state.journeyDots(def),
        footer = if (count != null) {
            { BadgeProgressBar(if (earnedOn != null) count.copy(done = count.total) else count, badgeAccent(def), Modifier.width(48.dp).height(3.dp)) }
        } else {
            null
        },
    )
}

@Composable
internal fun BadgeTile(
    def: BadgeDef,
    earned: Boolean,
    badgeSize: Dp,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    dots: List<Boolean>? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = status }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            BadgeHex(def, earned = earned, size = badgeSize, dots = dots)
            if (isNew) {
                Text(
                    stringResource(R.string.milestones_new),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Text(
            stringResource(badgeText(def).nameRes),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        footer?.invoke()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameBadgesCard(state: MilestonesState) {
    val gameBadges = BadgeCatalog.gameBadges
    val earnedCount = gameBadges.count { it.id in state.earned }
    val games = GameCatalog.games.filterNot { it.beta }.filter { BadgeCatalog.forGame(it.id).isNotEmpty() }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.milestones_game_badges, earnedCount, gameBadges.size), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.milestones_game_badges_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            games.forEach { meta ->
                val defs = BadgeCatalog.forGame(meta.id)
                Text(
                    stringResource(R.string.milestones_game_progress, stringResource(meta.titleRes), defs.count { it.id in state.earned }, defs.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
