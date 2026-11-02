package com.quietgrid.app.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.gameDifficultyLabelRes

@Composable
fun JourneyScreen(gameId: GameId, onOpenChapter: (Int) -> Unit) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsState(initial = null)
    val current = state ?: return
    val chapters = remember(current.rows) { journeyChapterSummaries(current.rows) }
    JourneyChapterList(gameId, chapters, onOpenChapter)
}

@Composable
fun JourneyChapterList(gameId: GameId, chapters: List<JourneyChapterSummary>, onOpenChapter: (Int) -> Unit) {
    val listState = rememberLazyListState()
    var positioned by rememberSaveable(gameId) { mutableStateOf(false) }
    LaunchedEffect(chapters) {
        if (positioned || chapters.isEmpty()) return@LaunchedEffect
        val inProgress = chapters.indexOfFirst { it.status == JourneyChapterStatus.IN_PROGRESS }
        if (inProgress > 0) listState.scrollToItem(inProgress)
        positioned = true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "note") {
                Text(
                    stringResource(R.string.journey_open_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            items(chapters, key = { "chapter-${it.index}" }) { chapter ->
                JourneyChapterCard(gameId, chapter, onClick = { onOpenChapter(chapter.index) })
            }
        }
    }
}

@Composable
private fun JourneyChapterCard(gameId: GameId, chapter: JourneyChapterSummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val subtitle = journeyChapterSubtitle(gameId, chapter)
    val statusLabel = stringResource(
        when (chapter.status) {
            JourneyChapterStatus.COMPLETE -> R.string.journey_chapter_complete
            JourneyChapterStatus.IN_PROGRESS -> R.string.journey_chapter_in_progress
            JourneyChapterStatus.OPEN -> R.string.journey_chapter_open
        },
    )
    val accent = if (chapter.status == JourneyChapterStatus.OPEN) colors.onSurfaceVariant else colors.primary
    val title = stringResource(R.string.journey_chapter, chapter.index)
    val description = stringResource(R.string.journey_chapter_description, chapter.index, statusLabel, chapter.stars, chapter.maxStars)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (chapter.status == JourneyChapterStatus.IN_PROGRESS) colors.surfaceContainerHighest else colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .semantics { contentDescription = description },
    ) {
        Row(
            Modifier
                .clearAndSetSemantics {}
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    chapter.index.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = accent)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    JourneyProgressBar(
                        fraction = if (chapter.total == 0) 0f else chapter.cleared.toFloat() / chapter.total,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(14.dp),
                    )
                    Text(
                        stringResource(R.string.journey_map_star_count, chapter.stars, chapter.maxStars),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun journeyChapterSubtitle(gameId: GameId, chapter: JourneyChapterSummary): String {
    val tierLabels = chapter.tiers.map { stringResource(gameDifficultyLabelRes(gameId, Difficulty.fromKey(it))) }
    val tiers = when {
        tierLabels.isEmpty() -> ""
        tierLabels.size == 1 -> tierLabels.first()
        else -> stringResource(R.string.journey_chapter_tiers, tierLabels.first(), tierLabels.last())
    }
    val stopCount = pluralStringResource(R.plurals.journey_chapter_stop_count, chapter.total, chapter.total)
    return listOf(stopCount, tiers).filter { it.isNotEmpty() }.joinToString(" " + stringResource(R.string.play_info_separator) + " ")
}

@Composable
private fun JourneyProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun JourneyStars(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(JOURNEY_MAX_STARS_PER_STOP) { index ->
            val earned = index < stars
            Icon(
                if (earned) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
