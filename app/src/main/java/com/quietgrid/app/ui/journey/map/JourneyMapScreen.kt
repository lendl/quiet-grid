package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.journeyScope
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.components.ReplacePuzzleSheet
import com.quietgrid.app.ui.components.rememberReduceMotion
import com.quietgrid.app.ui.journey.JourneyStopRow
import com.quietgrid.app.ui.journey.JourneyStopState
import com.quietgrid.app.ui.journey.JourneyViewModel
import com.quietgrid.app.ui.journey.journeyChapterSubtitle
import com.quietgrid.app.ui.journey.journeyChapterSummaries
import com.quietgrid.app.ui.journey.journeyRequestFor
import com.quietgrid.app.ui.theme.LocalResolvedTheme
import kotlin.math.roundToInt

const val JOURNEY_MAP_VIEWPORT_TAG = "journeyMapViewport"
const val JOURNEY_STOP_CARD_TAG = "journeyStopCard"
private val JOURNEY_MAP_PANE_MAX_WIDTH = 480.dp
private val JOURNEY_TWO_PANE_MIN_WIDTH = 600.dp
private val PEBBLE_SHAPE = RoundedCornerShape(topStartPercent = 46, topEndPercent = 54, bottomEndPercent = 52, bottomStartPercent = 48)

@Composable
fun JourneyMapScreen(
    gameId: GameId,
    chapter: Int,
    onPlayStop: (Difficulty, JourneyRequest) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val repositories: RepositoriesViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsState(initial = null)
    val activeSession by repositories.sessionRepository.activeSession.collectAsState(initial = null)
    val reduceMotion = rememberReduceMotion()
    var pendingRow by remember(gameId) { mutableStateOf<JourneyStopRow?>(null) }
    var selectedId by remember(gameId, chapter) { mutableStateOf<String?>(null) }

    fun play(row: JourneyStopRow) = onPlayStop(Difficulty.fromKey(row.stop.difficulty), journeyRequestFor(row))

    pendingRow?.let { row ->
        ReplacePuzzleSheet(
            onContinue = {
                pendingRow = null
                activeSession?.gameId?.let { key -> GameId.entries.firstOrNull { it.key == key } }?.let(onResumeActiveGame)
            },
            onStartNew = {
                pendingRow = null
                play(row)
            },
            onDismiss = { pendingRow = null },
        )
    }

    val rows = remember(state, chapter) { state?.rows?.filter { it.chapterIndex == chapter }.orEmpty() }
    if (rows.isEmpty()) return
    val selected = rows.firstOrNull { it.stop.puzzleId == selectedId }
        ?: rows.firstOrNull { it.state == JourneyStopState.CURRENT }
        ?: rows.first()
    val previewRequest = journeyRequestFor(selected)
    val preview by produceState<JourneyBoardPreviewData?>(null, gameId, selected.stop.difficulty, previewRequest) {
        value = null
        value = viewModel.previewFor(gameId, selected.stop.difficulty, previewRequest.puzzleId, previewRequest.transform)
    }
    val doneCount = remember(rows) { doneStopCount(rows) }
    val sceneKind = journeySceneFor(LocalResolvedTheme.current)
    val seed = remember(gameId, chapter) { journeySceneSeed(journeyScope(gameId), chapter) }
    JourneyMapContent(
        gameId = gameId,
        chapter = chapter,
        rows = rows,
        selectedStopId = selected.stop.puzzleId,
        onSelect = { selectedId = it },
        preview = preview,
        reduceMotion = reduceMotion,
        onPlay = { row -> if (activeSession != null) pendingRow = row else play(row) },
        onSkip = { row -> viewModel.skip(gameId, row.stop.puzzleId) },
        scenery = { layout -> JourneySceneCanvas(sceneKind, layout, doneCount, seed, Modifier.fillMaxSize()) },
        stopShape = if (sceneKind == JourneySceneKind.ZEN) PEBBLE_SHAPE else CircleShape,
    )
}

@Composable
fun JourneyMapContent(
    gameId: GameId,
    chapter: Int,
    rows: List<JourneyStopRow>,
    selectedStopId: String,
    onSelect: (String) -> Unit,
    preview: JourneyBoardPreviewData?,
    reduceMotion: Boolean,
    onPlay: (JourneyStopRow) -> Unit,
    onSkip: (JourneyStopRow) -> Unit,
    scenery: @Composable (JourneyRoadLayout) -> Unit = {},
    stopShape: Shape = CircleShape,
) {
    val selectedRow = rows.firstOrNull { it.stop.puzzleId == selectedStopId } ?: rows.firstOrNull() ?: return
    val colors = MaterialTheme.colorScheme

    @Composable
    fun mapPane(modifier: Modifier) = JourneyMapPane(
        gameId = gameId,
        chapter = chapter,
        rows = rows,
        selectedStopId = selectedRow.stop.puzzleId,
        onSelect = onSelect,
        reduceMotion = reduceMotion,
        scenery = scenery,
        stopShape = stopShape,
        modifier = modifier,
    )

    @Composable
    fun card(modifier: Modifier) = JourneyStopCard(
        row = selectedRow,
        preview = preview,
        onPlay = { onPlay(selectedRow) },
        onSkip = { onSkip(selectedRow) },
        modifier = modifier.testTag(JOURNEY_STOP_CARD_TAG),
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth > maxHeight || maxWidth >= JOURNEY_TWO_PANE_MIN_WIDTH
        if (twoPane) {
            val cardWidth = (maxWidth * 0.45f).coerceIn(300.dp, 400.dp)
            val mapWidth = (maxWidth - cardWidth).coerceAtMost(JOURNEY_MAP_PANE_MAX_WIDTH)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                mapPane(
                    Modifier
                        .width(mapWidth)
                        .fillMaxHeight(),
                )
                Column(
                    Modifier
                        .width(cardWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    card(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surfaceContainer),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                mapPane(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                card(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(colors.surfaceContainer),
                )
            }
        }
    }
}

@Composable
private fun JourneyMapPane(
    gameId: GameId,
    chapter: Int,
    rows: List<JourneyStopRow>,
    selectedStopId: String,
    onSelect: (String) -> Unit,
    reduceMotion: Boolean,
    scenery: @Composable (JourneyRoadLayout) -> Unit,
    stopShape: Shape,
    modifier: Modifier,
) {
    val summary = remember(rows) { journeyChapterSummaries(rows).firstOrNull() }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.journey_chapter, chapter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (summary != null) {
                    Text(
                        journeyChapterSubtitle(gameId, summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (summary != null) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.journey_map_star_count, summary.stars, summary.maxStars),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .testTag(JOURNEY_MAP_VIEWPORT_TAG),
        ) {
            val width = maxWidth
            val viewportPx = constraints.maxHeight
            val density = LocalDensity.current
            val layout = remember(rows.size, width) { journeyRoadLayout(rows.size, width.value) }
            val selectedIndex = rows.indexOfFirst { it.stop.puzzleId == selectedStopId }.coerceAtLeast(0)
            fun stopPx(index: Int): Float = with(density) { layout.stops[index].y.dp.toPx() }
            fun targetFor(index: Int): Int = (stopPx(index) - viewportPx / 2f).roundToInt().coerceAtLeast(0)
            val scrollState = rememberScrollState(initial = targetFor(selectedIndex))
            LaunchedEffect(selectedIndex, viewportPx, layout) {
                val margin = with(density) { 48.dp.toPx() }
                val y = stopPx(selectedIndex)
                val top = scrollState.value
                if (y - margin < top || y + margin > top + viewportPx) {
                    val target = targetFor(selectedIndex).coerceAtMost(scrollState.maxValue)
                    if (reduceMotion) scrollState.scrollTo(target) else scrollState.animateScrollTo(target)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                Box(
                    Modifier
                        .width(width)
                        .height(layout.height.dp),
                ) {
                    scenery(layout)
                    rows.forEachIndexed { index, row ->
                        if (row.state == JourneyStopState.CURRENT) {
                            val point = layout.stops[index]
                            JourneyStopHalo(Modifier.offset(x = (point.x - 46f).dp, y = (point.y - 46f).dp))
                        }
                    }
                    rows.forEachIndexed { index, row ->
                        val point = layout.stops[index]
                        Box(
                            Modifier
                                .offset(x = (point.x - JOURNEY_STOP_HALF_SLOT).dp, y = (point.y - 34f).dp)
                                .width((JOURNEY_STOP_HALF_SLOT * 2).dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            JourneyStopNode(
                                gameId = gameId,
                                row = row,
                                selected = row.stop.puzzleId == selectedStopId,
                                onClick = { onSelect(row.stop.puzzleId) },
                                shape = stopShape,
                            )
                        }
                    }
                }
            }
        }
    }
}
