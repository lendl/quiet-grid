package com.quietgrid.app.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatTotalTime
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.core.journey.journeyScope
import com.quietgrid.app.ui.journey.map.JourneySceneCanvas
import com.quietgrid.app.ui.journey.map.journeySceneFor
import com.quietgrid.app.ui.journey.map.journeySceneSeed
import com.quietgrid.app.ui.journey.map.smoothRoadPath
import com.quietgrid.app.ui.theme.LocalResolvedTheme
import com.quietgrid.app.ui.theme.celebrationGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MAX_CONTENT_WIDTH = 480.dp
private val CONTENT_TOP = 52.dp
private val CONTENT_BOTTOM = 24.dp
private val TEXT_PADDING = 24.dp
private val BLOCK_PADDING = 20.dp
private val BLOCK_GAP = 18.dp
private val CHROME_HEIGHT = 430.dp
private val MIN_MAP_HEIGHT = 220.dp
private val MAX_MAP_HEIGHT = 460.dp
private val MAP_TOP_ROOM = 12.dp
private val STOP_RADIUS = 13.dp
private val CHAPTER_NODE_RADIUS = 15.dp
private val DOT_RADIUS = 3.dp
private val DOT_LIT_RADIUS = 3.6.dp
private val GLOW_WIDTH = 11.dp
private val RING_STROKE = 2.dp
private val RAY_WIDTH = 2.dp
private val RAY_INNER_GAP = 6.dp
private val RAY_LONG = 15.dp
private val RAY_SHORT = 11.dp
private val LABEL_GAP = 11.dp
private val LABEL_STAR_RADIUS = 5.dp
private val LABEL_STAR_GAP = 4.dp
private const val SCRIM_ALPHA = 0.95f
private const val GLOW_ALPHA = 0.22f
private const val HALO_ALPHA = 0.14f
private const val HALO_SCALE = 1.55f
private const val LAST_STOP_SCALE = 1.3f
private const val STOP_SPACING_SHARE = 0.42f
private const val NODE_SPACING_SHARE = 0.3f
private const val MIN_NUMBERED_RADIUS_DP = 9f
private const val STAR_INNER = 0.5f
private const val STAR_SHARE = 0.55f
private const val RAY_COUNT = 12

data class JourneyCelebrationActions(
    val onClose: () -> Unit,
    val onPlay: (JourneyStopRow) -> Unit,
    val onMap: (Int) -> Unit,
    val onDone: () -> Unit,
)

private data class CelebrationNode(val label: String, val side: String? = null, val sideFull: Boolean = false)

private data class CountedStars(val stars: Int, val maxStars: Int?, val tint: Color)

@Composable
fun JourneyCelebration(
    gameId: GameId,
    milestone: JourneyMilestone,
    rows: List<JourneyStopRow>,
    playSeconds: Int,
    reduceMotion: Boolean,
    actions: JourneyCelebrationActions,
    modifier: Modifier = Modifier,
) {
    when (milestone) {
        is JourneyMilestone.ChapterComplete -> {
            val model = remember(rows, milestone) { chapterCelebrationModel(rows, milestone.chapterIndex) } ?: return
            ChapterCelebration(gameId, model, reduceMotion, actions, modifier)
        }
        JourneyMilestone.JourneyComplete, JourneyMilestone.Mastered -> {
            val model = remember(rows) { journeyCelebrationModel(rows) } ?: return
            WholeJourneyCelebration(gameId, model, playSeconds, milestone == JourneyMilestone.Mastered, reduceMotion, actions, modifier)
        }
    }
}

@Composable
private fun ChapterCelebration(
    gameId: GameId,
    model: ChapterCelebrationModel,
    reduceMotion: Boolean,
    actions: JourneyCelebrationActions,
    modifier: Modifier = Modifier,
) {
    val timeline = remember(model.stopCount) { CelebrationTimeline(chapterLightMs(model.stopCount)) }
    val elapsed = rememberCelebrationClock(timeline.totalMs, reduceMotion)
    val colors = MaterialTheme.colorScheme
    val gameTitle = stringResource(GameCatalog.get(gameId).titleRes)
    val nodes = remember(model.stopCount) { (1..model.stopCount).map { CelebrationNode(it.toString()) } }
    val description = stringResource(R.string.journey_chapter_road_description, model.chapterIndex, model.stopCount, model.stopCount)
    val timeLabel = stringResource(R.string.journey_stat_best_times)
    CelebrationLayout(
        elapsed = elapsed,
        timeline = timeline,
        label = stringResource(R.string.journey_chapter_complete_label, gameTitle, model.chapterIndex),
        title = stringResource(R.string.journey_chapter_complete_title, model.chapterIndex),
        subtitle = stringResource(R.string.journey_chapter_all_cleared, model.stopCount),
        onClose = actions.onClose,
        map = { mapModifier ->
            CelebrationMap(
                gameId = gameId,
                seedChapter = model.chapterIndex,
                nodes = nodes,
                dotsBefore = emptyList(),
                accent = colors.primary,
                onAccent = colors.onPrimary,
                rays = false,
                timeline = timeline,
                elapsed = elapsed,
                description = description,
                modifier = mapModifier,
            )
        },
        stats = { statsModifier ->
            CountingStats(
                elapsed = elapsed,
                timeline = timeline,
                stars = CountedStars(model.stars, model.maxStars, colors.primary),
                seconds = model.bestSeconds,
                secondsLabel = timeLabel,
                cleanBoards = model.cleanBoards,
                modifier = statsModifier,
            )
        },
        buttons = { buttonsModifier -> ChapterButtons(model, actions, buttonsModifier) },
        modifier = modifier,
    )
}

@Composable
private fun WholeJourneyCelebration(
    gameId: GameId,
    model: JourneyCelebrationModel,
    playSeconds: Int,
    mastered: Boolean,
    reduceMotion: Boolean,
    actions: JourneyCelebrationActions,
    modifier: Modifier = Modifier,
) {
    val timeline = remember { CelebrationTimeline(CELEBRATION_JOURNEY_LIGHT_MS) }
    val elapsed = rememberCelebrationClock(timeline.totalMs, reduceMotion)
    val colors = MaterialTheme.colorScheme
    val gold = celebrationGold()
    val accent = if (mastered) gold.gold else colors.primary
    val onAccent = if (mastered) gold.onGold else colors.onPrimary
    val gameTitle = stringResource(GameCatalog.get(gameId).titleRes)
    val nodes = model.chapters.map { chapter ->
        CelebrationNode(
            label = chapter.index.toString(),
            side = stringResource(R.string.journey_map_star_count, chapter.stars, chapter.maxStars),
            sideFull = chapter.stars >= chapter.maxStars,
        )
    }
    val dotsBefore = remember(model.chapters) { model.chapters.map { it.total } }
    val cleared = stringResource(R.string.journey_chapter_all_cleared, model.stopCount)
    val subtitle = when {
        mastered -> stringResource(R.string.journey_mastered_all_stops, model.stopCount)
        model.starsToGo > 0 -> listOf(
            cleared,
            pluralStringResource(R.plurals.journey_stars_to_gold, model.starsToGo, model.starsToGo),
        ).joinToString(" " + stringResource(R.string.play_info_separator) + " ")
        else -> cleared
    }
    val description = stringResource(R.string.journey_road_description, model.stopCount, model.stopCount)
    val timeLabel = stringResource(R.string.journey_stat_total_time)
    CelebrationLayout(
        elapsed = elapsed,
        timeline = timeline,
        label = stringResource(R.string.journey_mastered_label, gameTitle),
        title = stringResource(if (mastered) R.string.journey_mastered_title else R.string.journey_complete_title),
        subtitle = subtitle,
        onClose = actions.onClose,
        map = { mapModifier ->
            CelebrationMap(
                gameId = gameId,
                seedChapter = 0,
                nodes = nodes,
                dotsBefore = dotsBefore,
                accent = accent,
                onAccent = onAccent,
                rays = mastered,
                timeline = timeline,
                elapsed = elapsed,
                description = description,
                modifier = mapModifier,
            )
        },
        stats = { statsModifier ->
            CountingStats(
                elapsed = elapsed,
                timeline = timeline,
                stars = CountedStars(model.stars, model.maxStars.takeUnless { mastered }, accent),
                seconds = playSeconds,
                secondsLabel = timeLabel,
                cleanBoards = model.cleanBoards,
                modifier = statsModifier,
            )
        },
        buttons = { buttonsModifier -> WholeJourneyButtons(model, mastered, accent, onAccent, actions, buttonsModifier) },
        modifier = modifier,
    )
}

@Composable
private fun CelebrationLayout(
    elapsed: () -> Long,
    timeline: CelebrationTimeline,
    label: String,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    map: @Composable (Modifier) -> Unit,
    stats: @Composable (Modifier) -> Unit,
    buttons: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = timeline.fade(elapsed()) }
            .background(colors.background.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures() },
        contentAlignment = Alignment.TopCenter,
    ) {
        val mapHeight = (maxHeight - CHROME_HEIGHT).coerceIn(MIN_MAP_HEIGHT, MAX_MAP_HEIGHT)
        Column(
            Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = CONTENT_TOP, bottom = CONTENT_BOTTOM),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TEXT_PADDING).rise(elapsed, CELEBRATION_FADE_MS),
            )
            map(Modifier.fillMaxWidth().height(mapHeight + MAP_TOP_ROOM))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TEXT_PADDING).rise(elapsed, timeline.headlineMs),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = TEXT_PADDING, end = TEXT_PADDING, top = 6.dp).rise(elapsed, timeline.sublineMs),
            )
            stats(Modifier.padding(start = BLOCK_PADDING, end = BLOCK_PADDING, top = BLOCK_GAP).rise(elapsed, timeline.statsMs))
            buttons(
                Modifier
                    .padding(start = BLOCK_PADDING, end = BLOCK_PADDING, top = BLOCK_GAP)
                    .rise(elapsed, timeline.buttonsMs, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP),
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 4.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountingStats(
    elapsed: () -> Long,
    timeline: CelebrationTimeline,
    stars: CountedStars,
    seconds: Int,
    secondsLabel: String,
    cleanBoards: Int,
    modifier: Modifier = Modifier,
) {
    val now = elapsed()
    val shownStars = timeline.count(now, stars.stars)
    val starsValue = stars.maxStars?.let { stringResource(R.string.journey_map_star_count, shownStars, it) } ?: shownStars.toString()
    val starsSpoken = stars.maxStars?.let { stringResource(R.string.journey_map_star_count, stars.stars, it) } ?: stars.stars.toString()
    CelebrationStatsCard(
        stats = listOf(
            CelebrationStat(starsValue, stringResource(R.string.journey_stat_stars), starsSpoken, stars.tint),
            CelebrationStat(formatTotalTime(timeline.count(now, seconds)), secondsLabel, formatTotalTime(seconds)),
            CelebrationStat(
                timeline.count(now, cleanBoards).toString(),
                stringResource(R.string.journey_stat_clean_boards),
                cleanBoards.toString(),
            ),
        ),
        modifier = modifier,
    )
}

@Composable
private fun ChapterButtons(model: ChapterCelebrationModel, actions: JourneyCelebrationActions, modifier: Modifier = Modifier) {
    val next = model.nextChapterStop
    val replay = model.replayStop
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (next != null) {
            Button(onClick = { actions.onPlay(next) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text(stringResource(R.string.journey_start_chapter, next.chapterIndex), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (replay != null) {
                    OutlinedButton(onClick = { actions.onPlay(replay) }, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                        Text(stringResource(R.string.journey_replay_for_stars))
                    }
                }
                OutlinedButton(onClick = { actions.onMap(model.chapterIndex) }, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                    Text(stringResource(R.string.journey_map))
                }
            }
        } else {
            Button(onClick = { actions.onMap(model.chapterIndex) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text(stringResource(R.string.journey_map), fontWeight = FontWeight.Bold)
            }
            if (replay != null) {
                OutlinedButton(onClick = { actions.onPlay(replay) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text(stringResource(R.string.journey_replay_for_stars))
                }
            }
        }
    }
}

@Composable
private fun WholeJourneyButtons(
    model: JourneyCelebrationModel,
    mastered: Boolean,
    accent: Color,
    onAccent: Color,
    actions: JourneyCelebrationActions,
    modifier: Modifier = Modifier,
) {
    val replay = model.replayStop
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            mastered -> {
                Button(
                    onClick = actions.onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                ) {
                    Text(stringResource(R.string.journey_done), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { actions.onMap(model.lastChapterIndex) }, modifier = Modifier.weight(1f).heightIn(min = 54.dp)) {
                    Text(stringResource(R.string.journey_view_map))
                }
            }
            replay != null -> {
                Button(onClick = { actions.onPlay(replay) }, modifier = Modifier.weight(1f).heightIn(min = 54.dp)) {
                    Text(stringResource(R.string.journey_replay_for_stars), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = actions.onDone, modifier = Modifier.weight(1f).heightIn(min = 54.dp)) {
                    Text(stringResource(R.string.journey_done))
                }
            }
            else -> {
                Button(onClick = actions.onDone, modifier = Modifier.weight(1f).heightIn(min = 54.dp)) {
                    Text(stringResource(R.string.journey_done), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CelebrationMap(
    gameId: GameId,
    seedChapter: Int,
    nodes: List<CelebrationNode>,
    dotsBefore: List<Int>,
    accent: Color,
    onAccent: Color,
    rays: Boolean,
    timeline: CelebrationTimeline,
    elapsed: () -> Long,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val sceneKind = journeySceneFor(LocalResolvedTheme.current)
    val seed = remember(gameId, seedChapter) { journeySceneSeed(journeyScope(gameId), seedChapter) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = description }) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight - MAP_TOP_ROOM
        val road = remember(nodes.size, dotsBefore, mapWidth, mapHeight) {
            celebrationRoad(nodes.size, mapWidth.value, mapHeight.value, dotsBefore)
        }
        val step = mapHeight.value / (nodes.size + 1)
        val radiusDp = if (dotsBefore.isEmpty()) {
            minOf(STOP_RADIUS.value, step * STOP_SPACING_SHARE)
        } else {
            minOf(CHAPTER_NODE_RADIUS.value, step * NODE_SPACING_SHARE)
        }
        JourneySceneCanvas(
            kind = sceneKind,
            layout = road.layout,
            doneCount = nodes.size,
            seed = seed,
            modifier = Modifier
                .padding(top = MAP_TOP_ROOM)
                .size(mapWidth, mapHeight),
        )
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val glow = PathParser().parsePathString(smoothRoadPath(road.layout.full)).toPath()
                    val measure = PathMeasure().apply { setPath(glow, false) }
                    val lit = Path()
                    val radius = radiusDp.dp.toPx()
                    val lastRadius = radius * LAST_STOP_SCALE
                    val lastStar = polygonPath(starPolygon(Offset.Zero, lastRadius * STAR_SHARE, STAR_INNER))
                    val labelStar = polygonPath(starPolygon(Offset.Zero, LABEL_STAR_RADIUS.toPx(), STAR_INNER))
                    val numbers = nodes.map { node -> if (radiusDp >= MIN_NUMBERED_RADIUS_DP) textMeasurer.measure(node.label, labelStyle) else null }
                    val sides = nodes.map { node -> node.side?.let { textMeasurer.measure(it, labelStyle) } }
                    val glowStroke = Stroke(width = GLOW_WIDTH.value, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        val t = elapsed()
                        translate(top = MAP_TOP_ROOM.toPx()) {
                            val progress = timeline.lightProgress(t)
                            if (progress > 0f) {
                                lit.reset()
                                measure.getSegment(0f, measure.length * progress, lit, true)
                                scale(density, pivot = Offset.Zero) { drawPath(lit, accent, alpha = GLOW_ALPHA, style = glowStroke) }
                            }
                            road.dots.forEach { dot ->
                                val center = Offset(dot.point.x * density, dot.point.y * density)
                                if (timeline.lit(t, dot.fraction)) {
                                    drawCircle(accent, DOT_LIT_RADIUS.toPx() * timeline.popScale(t, dot.fraction), center)
                                } else {
                                    drawCircle(colors.primaryContainer, DOT_RADIUS.toPx(), center)
                                }
                            }
                            road.layout.stops.forEachIndexed { index, point ->
                                val center = Offset(point.x * density, point.y * density)
                                val fraction = road.stopFractions[index]
                                val last = index == road.layout.stops.lastIndex
                                val nodeRadius = if (last) lastRadius else radius
                                val on = timeline.lit(t, fraction)
                                scale(timeline.popScale(t, fraction), pivot = center) {
                                    if (on) {
                                        drawCircle(accent, nodeRadius * HALO_SCALE, center, alpha = HALO_ALPHA)
                                        drawCircle(accent, nodeRadius, center)
                                    } else {
                                        drawCircle(colors.primaryContainer, nodeRadius, center)
                                    }
                                    val number = numbers[index]
                                    if (last && on) {
                                        translate(center.x, center.y) { drawPath(lastStar, onAccent) }
                                    } else if (number != null) {
                                        drawCentered(number, center, if (on) onAccent else colors.onPrimaryContainer)
                                    }
                                }
                                val side = sides[index]
                                if (side != null) {
                                    val reach = nodeRadius + LABEL_GAP.toPx() + if (last && rays) RAY_LONG.toPx() else 0f
                                    val tint = if (nodes[index].sideFull) accent else colors.onSurfaceVariant
                                    drawSideLabel(side, labelStar, center, reach, point.x >= road.layout.width / 2, tint)
                                }
                            }
                            val top = road.layout.stops.lastOrNull()
                            if (top != null) {
                                val center = Offset(top.x * density, top.y * density)
                                val ring = timeline.ring(t)
                                if (ring.alpha > 0f) {
                                    drawCircle(accent, lastRadius + ring.growthDp.dp.toPx(), center, alpha = ring.alpha, style = Stroke(RING_STROKE.toPx()))
                                }
                                val raysShown = timeline.rays(t)
                                if (rays && raysShown > 0f) drawRays(center, lastRadius, accent, raysShown)
                            }
                        }
                    }
                },
        )
    }
}

private fun DrawScope.drawCentered(text: TextLayoutResult, center: Offset, color: Color) {
    drawText(text, color = color, topLeft = center - Offset(text.size.width / 2f, text.size.height / 2f))
}

private fun DrawScope.drawSideLabel(text: TextLayoutResult, star: Path, center: Offset, reach: Float, toRight: Boolean, color: Color) {
    val starRadius = LABEL_STAR_RADIUS.toPx()
    val gap = LABEL_STAR_GAP.toPx()
    val textLeft = if (toRight) center.x + reach + 2 * starRadius + gap else center.x - reach - text.size.width
    val starX = if (toRight) center.x + reach + starRadius else textLeft - gap - starRadius
    translate(starX, center.y) { drawPath(star, color) }
    drawText(text, color = color, topLeft = Offset(textLeft, center.y - text.size.height / 2f))
}

private fun DrawScope.drawRays(center: Offset, radius: Float, color: Color, alpha: Float) {
    val inner = radius + RAY_INNER_GAP.toPx()
    repeat(RAY_COUNT) { k ->
        val angle = k * 2 * PI / RAY_COUNT
        val outer = radius + (if (k % 2 == 0) RAY_LONG else RAY_SHORT).toPx()
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(color, center + direction * inner, center + direction * outer, RAY_WIDTH.toPx(), StrokeCap.Round, alpha = alpha)
    }
}
