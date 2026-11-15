package com.quietgrid.app.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatTotalTime
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.ui.components.rememberReduceMotion

private val ROAD_INSET = 24.dp
private val ROAD_ROW_GAP = 54.dp
private val ROAD_MARGIN = 16.dp
private val ROAD_WIDTH = 4.dp
private val ROAD_DASH = 1.dp
private val ROAD_GAP = 9.dp
private val STOP_RADIUS = 6.dp
private val LAST_STOP_RADIUS = 11.dp
private val LAST_STOP_STAR_RADIUS = 6.5.dp
private val STOP_STROKE = 2.dp
private val RING_STROKE = 2.dp
private const val LAST_STOP_STAR_INNER = 0.5f

@Composable
fun ChapterCelebrationScreen(
    gameId: GameId,
    chapter: Int,
    onClose: () -> Unit,
    onPlay: (Difficulty, JourneyRequest) -> Unit,
    onContinue: (JourneyMilestone) -> Unit,
) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsStateWithLifecycle(initialValue = null)
    val milestone = JourneyMilestone.ChapterComplete(chapter)
    val pending by produceState<List<JourneyMilestone>?>(initialValue = null, viewModel, gameId) {
        value = viewModel.pendingMilestones(gameId)
    }
    LaunchedEffect(viewModel, gameId, chapter) { viewModel.markShown(gameId, milestone) }
    val rows = state?.rows ?: return
    val others = pending ?: return
    val model = remember(rows, chapter) { chapterCelebrationModel(rows, chapter) }
    if (model == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    fun play(row: JourneyStopRow) = onPlay(Difficulty.fromKey(row.stop.difficulty), journeyRequestFor(row))
    ChapterCelebrationContent(
        gameId = gameId,
        model = model,
        reduceMotion = rememberReduceMotion(),
        next = others.firstOrNull { it != milestone },
        onClose = onClose,
        onStartNext = { model.nextChapterStop?.let(::play) },
        onReplay = { model.replayStop?.let(::play) },
        onContinue = onContinue,
    )
}

@Composable
fun ChapterCelebrationContent(
    gameId: GameId,
    model: ChapterCelebrationModel,
    reduceMotion: Boolean,
    next: JourneyMilestone?,
    onClose: () -> Unit,
    onStartNext: () -> Unit,
    onReplay: () -> Unit,
    onContinue: (JourneyMilestone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsed = rememberCelebrationClock(CHAPTER_CELEBRATION_TOTAL_MS, reduceMotion)
    val colors = MaterialTheme.colorScheme
    val gameTitle = stringResource(GameCatalog.get(gameId).titleRes)
    val roadDescription = stringResource(R.string.journey_chapter_road_description, model.chapterIndex, model.clearedCount, model.stopCount)
    CelebrationScaffold(onClose = onClose, modifier = modifier) {
        Text(
            stringResource(R.string.journey_chapter_complete_label, gameTitle, model.chapterIndex),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ChapterRoad(
            stopCount = model.stopCount,
            elapsedMs = elapsed,
            modifier = Modifier.clearAndSetSemantics { contentDescription = roadDescription },
        )
        Text(
            stringResource(R.string.journey_chapter_complete_title, model.chapterIndex),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(elapsed, CHAPTER_HEADLINE_MS),
        )
        Text(
            stringResource(R.string.journey_chapter_all_cleared, model.stopCount),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(elapsed, CHAPTER_SUBLINE_MS),
        )
        ChapterStats(model, elapsed, Modifier.rise(elapsed, CHAPTER_STATS_MS))
        if (model.canEarnMore > 0) {
            Text(
                pluralStringResource(R.plurals.journey_stops_can_earn_more, model.canEarnMore, model.canEarnMore),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.rise(elapsed, CHAPTER_NOTE_MS),
            )
        }
        Spacer(Modifier.height(8.dp))
        ChapterButtons(model, next, elapsed, onStartNext, onReplay, onClose, onContinue)
    }
}

@Composable
private fun ChapterStats(model: ChapterCelebrationModel, elapsedMs: () -> Long, modifier: Modifier = Modifier) {
    val shownStars = chapterStarCount(elapsedMs(), model.stars)
    CelebrationStatsCard(
        stats = listOf(
            CelebrationStat(
                value = stringResource(R.string.journey_map_star_count, shownStars, model.maxStars),
                label = stringResource(R.string.journey_stat_stars),
                spokenValue = stringResource(R.string.journey_map_star_count, model.stars, model.maxStars),
                starTint = MaterialTheme.colorScheme.primary,
            ),
            CelebrationStat(formatTotalTime(model.bestSeconds), stringResource(R.string.journey_stat_best_times)),
            CelebrationStat(model.cleanBoards.toString(), stringResource(R.string.journey_stat_clean_boards)),
        ),
        modifier = modifier,
    )
}

@Composable
private fun ChapterButtons(
    model: ChapterCelebrationModel,
    next: JourneyMilestone?,
    elapsedMs: () -> Long,
    onStartNext: () -> Unit,
    onReplay: () -> Unit,
    onMap: () -> Unit,
    onContinue: (JourneyMilestone) -> Unit,
) {
    val primary = Modifier
        .rise(elapsedMs, CHAPTER_PRIMARY_MS, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP)
        .fillMaxWidth()
        .heightIn(min = 54.dp)
    val secondary = Modifier.rise(elapsedMs, CHAPTER_SECONDARY_MS, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP)
    val nextStop = model.nextChapterStop
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            next != null -> Button(onClick = { onContinue(next) }, modifier = primary) {
                Text(stringResource(R.string.journey_continue), fontWeight = FontWeight.Bold)
            }
            nextStop != null -> {
                Button(onClick = onStartNext, modifier = primary) {
                    Text(stringResource(R.string.journey_start_chapter, nextStop.chapterIndex), fontWeight = FontWeight.Bold)
                }
                Row(secondary.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (model.canEarnMore > 0) {
                        OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                            Text(stringResource(R.string.journey_replay_for_stars))
                        }
                    }
                    OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                        Text(stringResource(R.string.journey_map))
                    }
                }
            }
            else -> {
                Button(onClick = onMap, modifier = primary) {
                    Text(stringResource(R.string.journey_map), fontWeight = FontWeight.Bold)
                }
                if (model.canEarnMore > 0) {
                    OutlinedButton(onClick = onReplay, modifier = secondary.fillMaxWidth().heightIn(min = 50.dp)) {
                        Text(stringResource(R.string.journey_replay_for_stars))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRoad(stopCount: Int, elapsedMs: () -> Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val rowCount = if (stopCount <= 0) 1 else (stopCount - 1) / CHAPTER_ROAD_STOPS_PER_ROW + 1
    Spacer(
        modifier
            .fillMaxWidth()
            .height(ROAD_MARGIN * 2 + ROAD_ROW_GAP * (rowCount - 1))
            .drawWithCache {
                val rowGap = ROAD_ROW_GAP.toPx()
                val layout = chapterRoadLayout(stopCount, size.width, ROAD_INSET.toPx(), rowGap, ROAD_MARGIN.toPx())
                val road = roadPath(layout, rowGap)
                val measure = PathMeasure().apply { setPath(road, false) }
                val lit = Path()
                val roadStroke = Stroke(width = ROAD_WIDTH.toPx(), cap = StrokeCap.Round)
                val dottedStroke = Stroke(
                    width = ROAD_WIDTH.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(ROAD_DASH.toPx(), ROAD_GAP.toPx())),
                )
                val stopStroke = Stroke(width = STOP_STROKE.toPx())
                val last = layout.points.lastOrNull()
                val lastStar = last?.let { polygonPath(starPolygon(it, LAST_STOP_STAR_RADIUS.toPx(), LAST_STOP_STAR_INNER)) }
                onDrawBehind {
                    val t = elapsedMs()
                    drawPath(road, colors.outlineVariant, style = dottedStroke)
                    lit.reset()
                    measure.getSegment(0f, measure.length * chapterRoadProgress(t), lit, true)
                    drawPath(lit, colors.primary, style = roadStroke)
                    val ring = chapterRingFrame(t)
                    if (last != null && ring.alpha > 0f) {
                        drawCircle(colors.primary, radius = ring.radiusDp.dp.toPx(), center = last, alpha = ring.alpha, style = Stroke(RING_STROKE.toPx()))
                    }
                    layout.points.forEachIndexed { i, point ->
                        val fraction = layout.fractions[i]
                        val isLast = i == layout.points.lastIndex
                        val radius = (if (isLast) LAST_STOP_RADIUS else STOP_RADIUS).toPx()
                        val on = chapterStopLit(t, fraction)
                        scale(chapterStopScale(t, fraction), pivot = point) {
                            if (on) {
                                drawCircle(colors.primary, radius, point)
                                if (isLast && lastStar != null) drawPath(lastStar, colors.onPrimary)
                            } else {
                                drawCircle(colors.surface, radius, point)
                                drawCircle(colors.outlineVariant, radius, point, style = stopStroke)
                            }
                        }
                    }
                }
            },
    )
}

private fun roadPath(layout: ChapterRoadLayout, rowGap: Float): Path {
    val path = Path()
    val points = layout.points
    if (points.isEmpty()) return path
    val radius = rowGap / 2
    path.moveTo(points.first().x, points.first().y)
    points.chunked(CHAPTER_ROAD_STOPS_PER_ROW).forEachIndexed { row, rowPoints ->
        val end = rowPoints.last()
        path.lineTo(end.x, end.y)
        if (rowPoints.size == CHAPTER_ROAD_STOPS_PER_ROW && (row + 1) * CHAPTER_ROAD_STOPS_PER_ROW < points.size) {
            val turnRight = row % 2 == 0
            path.arcTo(
                rect = Rect(Offset(end.x - radius, end.y), Offset(end.x + radius, end.y + rowGap)),
                startAngleDegrees = -90f,
                sweepAngleDegrees = if (turnRight) 180f else -180f,
                forceMoveTo = false,
            )
        }
    }
    return path
}

fun polygonPath(points: List<Offset>): Path = Path().apply {
    points.firstOrNull()?.let { moveTo(it.x, it.y) }
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}
