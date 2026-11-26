package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.appViewModel
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.core.badges.BadgeCount
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
private val GAME_HEX_SIZE = 34.dp

@Composable
fun MilestonesScreen(
    modifier: Modifier = Modifier,
    viewModel: MilestonesViewModel = appViewModel { graph -> MilestonesViewModel(graph.badgeRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!state.loaded) return

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.milestones_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MilestonesSummaryCard(state, onOpen = { openId = it })
            MILESTONE_SECTIONS
                .filter { (section, _) -> section != BadgeSection.SEASONAL || BadgeCatalog.section(section).any { it.id in state.earned } }
                .forEach { (section, titleRes) ->
                    MilestonesSection(stringResource(titleRes), BadgeCatalog.section(section), state, onOpen = { openId = it })
                }
            GameBadgesSection(state, onOpen = { openId = it })
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
    val all = BadgeCatalog.all
    val earnedAll = all.count { it.id in state.earned }
    val recent = state.earned.keys.toList().takeLast(RECENT_COUNT).reversed().mapNotNull { BadgeCatalog.byId(it) }
    val shape = RoundedCornerShape(18.dp)
    val total = stringResource(R.string.milestones_earned_total, all.size)
    val scheme = MaterialTheme.colorScheme
    val totalStyle = MaterialTheme.typography.titleMedium.toSpanStyle().copy(color = scheme.onSurfaceVariant)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(MaterialTheme.typography.displaySmall.toSpanStyle().copy(color = scheme.primary, fontWeight = FontWeight.Black)) {
                        append(earnedAll.toString())
                    }
                    withStyle(totalStyle) { append(" $total") }
                },
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                stringResource(R.string.milestones_earned_label),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            BadgeProgressBar(
                BadgeCount(done = earnedAll, total = all.size),
                scheme.primary,
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(4.dp),
            )
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.milestones_recent),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
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
private fun BadgeSectionHeader(title: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
        Text(
            stringResource(R.string.stats_badges_count, done, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .alignByBaseline(),
        )
    }
}

@Composable
private fun MilestonesSection(title: String, defs: List<BadgeDef>, state: MilestonesState, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BadgeSectionHeader(title, defs.count { it.id in state.earned }, defs.size)
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
        showStatus = earnedOn != null || count != null,
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
    showStatus: Boolean = earned,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(badgeText(def).nameRes),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showStatus) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        footer?.invoke()
    }
}

@Composable
private fun GameBadgesSection(state: MilestonesState, onOpen: (String) -> Unit) {
    val gameBadges = BadgeCatalog.gameBadges
    val games = GameCatalog.games.filterNot { it.beta }.filter { BadgeCatalog.forGame(it.id).isNotEmpty() }
    Column {
        BadgeSectionHeader(
            stringResource(R.string.milestones_group_games),
            gameBadges.count { it.id in state.earned },
            gameBadges.size,
            Modifier.padding(bottom = 4.dp),
        )
        games.forEachIndexed { index, meta ->
            if (index > 0) HorizontalDivider()
            GameBadgesLine(stringResource(meta.titleRes), BadgeCatalog.forGame(meta.id), state, onOpen)
        }
    }
}

@Composable
private fun GameBadgesLine(title: String, defs: List<BadgeDef>, state: MilestonesState, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(
                stringResource(R.string.stats_badges_count, defs.count { it.id in state.earned }, defs.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            defs.forEach { def -> GameBadgeHex(def, state, onOpen) }
        }
    }
}

@Composable
private fun GameBadgeHex(def: BadgeDef, state: MilestonesState, onOpen: (String) -> Unit) {
    val earnedOn = state.earned[def.id]
    val name = stringResource(badgeText(def).nameRes)
    val status = badgeStatusText(earnedOn, badgeCount(def.id, state.progress, state.journeys))
    BadgeHex(
        def = def,
        earned = earnedOn != null,
        size = GAME_HEX_SIZE,
        dots = state.journeyDots(def),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = name) { onOpen(def.id) }
            .semantics {
                contentDescription = name
                stateDescription = status
            },
    )
}
