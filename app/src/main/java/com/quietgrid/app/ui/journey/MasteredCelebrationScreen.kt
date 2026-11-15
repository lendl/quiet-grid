package com.quietgrid.app.ui.journey

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatTotalTime
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.ui.components.rememberReduceMotion
import com.quietgrid.app.ui.theme.celebrationGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val CONSTELLATION_HEIGHT = 300.dp
private val CONSTELLATION_MAX_RADIUS = 138.dp
private val NODE_RADIUS = 12.dp
private val NODE_STROKE = 2.dp
private val LINE_WIDTH = 2.dp
private val GOLD_OUTLINE_WIDTH = 2.5.dp
private val CENTER_STAR_RADIUS = 30.dp
private val HALO_INNER = 40.dp
private val HALO_OUTER_LONG = 52.dp
private val HALO_OUTER_SHORT = 46.dp
private val HALO_WIDTH = 2.dp
private const val CENTER_STAR_INNER = 0.5f
private const val HALO_RAYS = 12
private const val HALO_TURN_MS = 60_000
private val TWINKLE_CYCLE_MS = (2 * PI * TWINKLE_PERIOD_DIVISOR_MS).toInt()

private class AmbientMotion(val haloDegrees: State<Float>, val twinkleRadians: State<Float>?)

@Composable
fun MasteredCelebrationScreen(
    gameId: GameId,
    replay: Boolean,
    onDone: () -> Unit,
    onViewMap: (Int) -> Unit,
) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(viewModel, gameId, replay) {
        if (!replay) viewModel.markShown(gameId, JourneyMilestone.Mastered)
    }
    val rows = state?.rows ?: return
    val model = remember(rows) { masteredCelebrationModel(rows) }
    if (model == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    MasteredCelebrationContent(
        gameId = gameId,
        model = model,
        reduceMotion = rememberReduceMotion(),
        onDone = onDone,
        onViewMap = { onViewMap(model.lastChapterIndex) },
    )
}

@Composable
fun MasteredCelebrationContent(
    gameId: GameId,
    model: MasteredCelebrationModel,
    reduceMotion: Boolean,
    onDone: () -> Unit,
    onViewMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsed = rememberCelebrationClock(MASTERED_CELEBRATION_TOTAL_MS, reduceMotion)
    val colors = MaterialTheme.colorScheme
    val gold = celebrationGold()
    val gameTitle = stringResource(GameCatalog.get(gameId).titleRes)
    val chapterCount = model.chapterIndexes.size
    val description = pluralStringResource(R.plurals.journey_constellation_description, chapterCount, chapterCount)
    CelebrationScaffold(onClose = onDone, modifier = modifier) {
        Text(
            stringResource(R.string.journey_mastered_label, gameTitle),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Constellation(
            gameId = gameId,
            chapterIndexes = model.chapterIndexes,
            elapsedMs = elapsed,
            reduceMotion = reduceMotion,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
        Text(
            stringResource(R.string.journey_mastered_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(elapsed, MASTERED_HEADLINE_MS),
        )
        Text(
            stringResource(R.string.journey_mastered_sub, gameTitle, model.stars),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(elapsed, MASTERED_SUBLINE_MS),
        )
        CelebrationStatsCard(
            stats = listOf(
                CelebrationStat(chapterCount.toString(), stringResource(R.string.journey_stat_chapters)),
                CelebrationStat(model.stopCount.toString(), stringResource(R.string.journey_stat_stops)),
                CelebrationStat(formatTotalTime(model.bestSeconds), stringResource(R.string.journey_stat_best_times)),
            ),
            modifier = Modifier.rise(elapsed, MASTERED_STATS_MS),
        )
        Row(
            Modifier.rise(elapsed, MASTERED_NOTE_MS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = gold.gold, modifier = Modifier.padding(end = 6.dp).size(16.dp))
            Text(
                stringResource(R.string.journey_mastered_note, gameTitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = gold.gold, contentColor = gold.onGold),
                modifier = Modifier
                    .rise(elapsed, MASTERED_PRIMARY_MS, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP)
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Text(stringResource(R.string.journey_done), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onViewMap,
                modifier = Modifier
                    .rise(elapsed, MASTERED_SECONDARY_MS, CELEBRATION_BUTTON_RISE_MS, CELEBRATION_BUTTON_RISE_DP)
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
            ) {
                Text(stringResource(R.string.journey_view_map))
            }
        }
    }
}

@Composable
private fun rememberAmbientMotion(enabled: Boolean): AmbientMotion {
    if (!enabled) return remember { AmbientMotion(mutableFloatStateOf(0f), null) }
    val transition = rememberInfiniteTransition(label = "constellation")
    val halo = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(HALO_TURN_MS, easing = LinearEasing)), label = "halo")
    val twinkle = transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(TWINKLE_CYCLE_MS, easing = LinearEasing)), label = "twinkle")
    return remember(halo, twinkle) { AmbientMotion(halo, twinkle) }
}

@Composable
private fun Constellation(
    gameId: GameId,
    chapterIndexes: List<Int>,
    elapsedMs: () -> Long,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val gold = celebrationGold()
    val motion = rememberAmbientMotion(enabled = !reduceMotion)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    val labels = remember(textMeasurer, chapterIndexes, labelStyle) { chapterIndexes.map { textMeasurer.measure(it.toString(), labelStyle) } }
    val sky = remember(gameId) { starField(gameId.key.hashCode()) }
    val count = chapterIndexes.size
    Spacer(
        modifier
            .fillMaxWidth()
            .height(CONSTELLATION_HEIGHT)
            .drawWithCache {
                val center = Offset(size.width / 2, size.height / 2)
                val outline = constellationOutline(center, constellationOuterRadius(size.width, CONSTELLATION_MAX_RADIUS.toPx()))
                val outlinePath = polygonPath(outline)
                val measure = PathMeasure().apply { setPath(outlinePath, true) }
                val nodes = constellationNodes(outline, count)
                val lines = Path()
                val centerStar = polygonPath(starPolygon(Offset.Zero, CENTER_STAR_RADIUS.toPx(), CENTER_STAR_INNER))
                val lineStroke = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val goldStroke = Stroke(width = GOLD_OUTLINE_WIDTH.toPx(), join = StrokeJoin.Round)
                val nodeStroke = Stroke(width = NODE_STROKE.toPx())
                val nodeRadius = NODE_RADIUS.toPx()
                onDrawBehind {
                    val t = elapsedMs()
                    val twinkle = motion.twinkleRadians?.value
                    sky.forEach { dot ->
                        drawCircle(
                            colors.onSurfaceVariant,
                            radius = dot.radiusDp.dp.toPx(),
                            center = Offset(dot.x * size.width, dot.y * size.height),
                            alpha = twinkleAlpha(dot, twinkle).coerceIn(0f, 1f),
                        )
                    }
                    val goldShown = masteredGoldProgress(t)
                    if (goldShown > 0f) drawPath(outlinePath, gold.gold, alpha = MASTERED_GOLD_FILL_ALPHA * goldShown)
                    val lineAlpha = masteredLineAlpha(t)
                    if (count > 0 && lineAlpha > 0f) {
                        lines.reset()
                        val step = measure.length / count
                        repeat(count) { k ->
                            val progress = masteredLineProgress(t, k, count)
                            if (progress > 0f) measure.getSegment(k * step, (k + progress) * step, lines, true)
                        }
                        drawPath(lines, colors.primary, alpha = lineAlpha, style = lineStroke)
                    }
                    if (goldShown > 0f) drawPath(outlinePath, gold.gold, alpha = goldShown, style = goldStroke)
                    val haloAlpha = masteredHaloAlpha(t)
                    if (haloAlpha > 0f) {
                        rotate(motion.haloDegrees.value, pivot = center) {
                            repeat(HALO_RAYS) { k ->
                                val angle = k * 2 * PI / HALO_RAYS
                                val outer = (if (k % 2 == 0) HALO_OUTER_LONG else HALO_OUTER_SHORT).toPx()
                                val inner = HALO_INNER.toPx()
                                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                                drawLine(gold.gold, center + direction * inner, center + direction * outer, HALO_WIDTH.toPx(), StrokeCap.Round, alpha = haloAlpha)
                            }
                        }
                    }
                    val star = masteredCenterStarFrame(t)
                    if (star.alpha > 0f) {
                        translate(center.x, center.y) {
                            rotate(star.rotationDegrees, pivot = Offset.Zero) {
                                scale(star.scale, pivot = Offset.Zero) {
                                    drawPath(centerStar, gold.gold, alpha = star.alpha)
                                }
                            }
                        }
                    }
                    nodes.forEachIndexed { k, node ->
                        val frame = masteredNodeFrame(t, k, count)
                        val fill = when {
                            frame.gold -> gold.gold
                            frame.lit -> colors.primary
                            else -> colors.background
                        }
                        val ink = when {
                            frame.gold -> gold.onGold
                            frame.lit -> colors.onPrimary
                            else -> colors.onSurfaceVariant
                        }
                        scale(frame.scale, pivot = node) {
                            drawCircle(fill, nodeRadius, node)
                            if (!frame.lit) drawCircle(colors.outlineVariant, nodeRadius, node, style = nodeStroke)
                            val label = labels[k]
                            drawText(label, color = ink, topLeft = node - Offset(label.size.width / 2f, label.size.height / 2f))
                        }
                    }
                }
            },
    )
}
