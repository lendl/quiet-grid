package com.quietgrid.app.games.blockfill

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.BadgePill
import com.quietgrid.app.ui.components.ConfettiBurst
import com.quietgrid.app.ui.components.CrownIcon
import com.quietgrid.app.ui.components.OutlinedGlowButton
import com.quietgrid.app.ui.components.systemAnimationsDisabled
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PAGE_FADE_MS = 220
private const val CONTENT_SLIDE_MS = 320
private const val CONTENT_SLIDE_PX = 24f
private const val FADE_MS = 250
private const val PILLS_DELAY_MS = 400L
private const val SCORE_DELAY_MS = 200L
private const val SCORE_COUNT_MS = 750
private const val NEW_BEST_SCORE_COUNT_MS = 1100
private const val POP_START_SCALE = 0.6f
private const val NEW_BEST_EMOJI = "👑"

private val MEDAL_ZONE_HEIGHT = 150.dp
private val MEDAL_GLOW_SIZE = 180.dp
private val MEDAL_SIZE = 96.dp
private val MEDAL_CROWN_SIZE = 44.dp
private val MEDAL_TILE_SIZE = 18.dp
private val MEDAL_TILE_GAP = 3.dp
private val BEST_BAR_WIDTH = 200.dp
private val BEST_BAR_HEIGHT = 4.dp
private val BEST_TICK_WIDTH = 2.dp
private val BEST_TICK_HEIGHT = 12.dp

private val MEDAL_TILE_FAMILIES = listOf(
    BlockFillShapeFamily.PLUS,
    BlockFillShapeFamily.CORNER_TROMINO,
    BlockFillShapeFamily.SQUARE2X2,
    BlockFillShapeFamily.STRAIGHT3,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockFillEndlessResultScreen(
    result: BlockFillEndlessResult,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember { systemAnimationsDisabled(context) }
    val isDarkTheme = LocalIsDarkTheme.current
    val hasPreviousBest = result.previousBest > 0
    val celebrateAbove = if (hasPreviousBest) result.previousBest else result.score - 1

    val pageOpacity = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val contentOffsetY = remember { Animatable(if (reduceMotion) 0f else CONTENT_SLIDE_PX) }
    val medalScale = remember { Animatable(if (reduceMotion) 1f else POP_START_SCALE) }
    val pillsOpacity = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val scoreProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val bestLineOpacity = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val crownProgress = remember { Animatable(0f) }
    var showConfetti by remember { mutableStateOf(false) }
    val celebrated by remember(result) {
        derivedStateOf { result.isNewBest && (result.score * scoreProgress.value).roundToInt() > celebrateAbove }
    }

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        launch { pageOpacity.animateTo(1f, tween(PAGE_FADE_MS)) }
        launch { contentOffsetY.animateTo(0f, tween(CONTENT_SLIDE_MS, easing = FastOutSlowInEasing)) }
        launch {
            medalScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        launch {
            delay(PILLS_DELAY_MS)
            pillsOpacity.animateTo(1f, tween(FADE_MS))
        }
        delay(SCORE_DELAY_MS)
        val countMs = if (result.isNewBest) NEW_BEST_SCORE_COUNT_MS else SCORE_COUNT_MS
        scoreProgress.animateTo(1f, tween(countMs, easing = FastOutSlowInEasing))
        bestLineOpacity.animateTo(1f, tween(FADE_MS))
    }
    LaunchedEffect(celebrated) {
        if (!celebrated) return@LaunchedEffect
        if (reduceMotion) {
            crownProgress.snapTo(1f)
        } else {
            showConfetti = true
            crownProgress.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    val titleRes = if (result.reason == BLOCKFILL_ENDLESS_REASON_STUCK) {
        R.string.blockfill_endless_result_stuck_title
    } else {
        R.string.blockfill_endless_result_abandoned_title
    }
    val bodyRes = when {
        result.isNewBest && hasPreviousBest -> R.string.blockfill_endless_result_new_best_body
        result.isNewBest -> R.string.blockfill_endless_result_first_run_body
        result.reason == BLOCKFILL_ENDLESS_REASON_STUCK -> R.string.blockfill_endless_result_stuck_body
        else -> R.string.blockfill_endless_result_abandoned_body
    }
    val bestGap = result.previousBest - result.score
    val bestLine = when {
        !hasPreviousBest -> null
        result.isNewBest -> stringResource(R.string.blockfill_endless_result_over_best, -bestGap)
        bestGap > 0 -> stringResource(R.string.blockfill_endless_result_short_of_best, bestGap)
        else -> null
    }
    val accentColor = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize()) {
        FlowRow(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer { alpha = pillsOpacity.value },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier
                    .border(1.dp, accentColor, CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).background(accentColor, CircleShape))
                Text(stringResource(R.string.blockfill_endless_label), style = MaterialTheme.typography.labelMedium, color = accentColor)
            }
            if (celebrated) {
                BadgePill(
                    emoji = NEW_BEST_EMOJI,
                    text = stringResource(R.string.blockfill_endless_result_new_best),
                    borderColor = accentColor.copy(alpha = 0.35f),
                    textColor = accentColor,
                    modifier = Modifier.graphicsLayer { alpha = crownProgress.value.coerceIn(0f, 1f) },
                )
            }
        }

        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .graphicsLayer {
                        alpha = pageOpacity.value
                        translationY = contentOffsetY.value
                    }
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EndlessMedallion(
                    medalScale = { medalScale.value },
                    crownProgress = { crownProgress.value },
                    showGlow = isDarkTheme,
                )

                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    stringResource(R.string.blockfill_endless_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.completion_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    EndlessCountUpScore(score = result.score, progress = { scoreProgress.value })

                    if (hasPreviousBest) {
                        EndlessBestBar(
                            score = result.score,
                            previousBest = result.previousBest,
                            isNewBest = result.isNewBest,
                            celebrated = celebrated,
                            progress = { scoreProgress.value },
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    if (bestLine != null) {
                        Text(
                            bestLine,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (result.isNewBest) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (result.isNewBest) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .graphicsLayer { alpha = bestLineOpacity.value },
                        )
                    }

                    Row(
                        Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetaItem(
                            stringResource(R.string.blockfill_endless_result_level),
                            stringResource(R.string.blockfill_multiplier_value, result.levelReached),
                        )
                        MetaDivider()
                        MetaItem(stringResource(R.string.blockfill_endless_result_lines), result.linesCleared.toString())
                        MetaDivider()
                        MetaItem(stringResource(R.string.blockfill_endless_result_moves), result.moves.toString())
                    }
                }

                OutlinedGlowButton(onClick = onPlayAgain, modifier = Modifier.padding(top = 24.dp)) {
                    Text(stringResource(R.string.blockfill_endless_play_again), fontWeight = FontWeight.Bold)
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.blockfill_endless_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    TextButton(onClick = onTryAnotherGame) {
                        Text(stringResource(R.string.completion_try_another_game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showConfetti) {
            ConfettiBurst(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun EndlessMedallion(medalScale: () -> Float, crownProgress: () -> Float, showGlow: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(MEDAL_ZONE_HEIGHT), contentAlignment = Alignment.Center) {
        if (showGlow) {
            Box(
                Modifier
                    .size(MEDAL_GLOW_SIZE)
                    .graphicsLayer { scaleX = medalScale(); scaleY = medalScale() }
                    .background(Brush.radialGradient(listOf(primary.copy(alpha = 0.35f), Color.Transparent)), CircleShape),
            )
        }
        Box(
            Modifier
                .size(MEDAL_SIZE)
                .graphicsLayer { scaleX = medalScale(); scaleY = medalScale() }
                .background(primary.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MedalTiles(Modifier.graphicsLayer { alpha = (1f - crownProgress()).coerceIn(0f, 1f) })
            CrownIcon(
                Modifier
                    .size(MEDAL_CROWN_SIZE)
                    .graphicsLayer {
                        val progress = crownProgress()
                        val scale = POP_START_SCALE + (1f - POP_START_SCALE) * progress
                        alpha = progress.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    },
                tint = primary,
            )
        }
    }
}

@Composable
private fun MedalTiles(modifier: Modifier = Modifier) {
    val colors = MEDAL_TILE_FAMILIES.map { blockFillFamilyColor(it) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MEDAL_TILE_GAP)) {
        colors.chunked(2).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(MEDAL_TILE_GAP)) {
                rowColors.forEach { BlockFillCell(it, MEDAL_TILE_SIZE) }
            }
        }
    }
}

@Composable
private fun EndlessCountUpScore(score: Int, progress: () -> Float) {
    Text(
        (score * progress()).roundToInt().toString(),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun EndlessBestBar(
    score: Int,
    previousBest: Int,
    isNewBest: Boolean,
    celebrated: Boolean,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val scale = if (isNewBest) score else previousBest
    val tickFraction = previousBest.toFloat() / scale
    val fillColor by animateColorAsState(
        if (celebrated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(FADE_MS),
        label = "endlessBestBarFill",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    Canvas(modifier.size(BEST_BAR_WIDTH, BEST_TICK_HEIGHT)) {
        val barHeight = BEST_BAR_HEIGHT.toPx()
        val barTop = (size.height - barHeight) / 2f
        val barRadius = CornerRadius(barHeight / 2f)
        drawRoundRect(trackColor, Offset(0f, barTop), Size(size.width, barHeight), barRadius)
        val fillFraction = (score * progress() / scale).coerceIn(0f, 1f)
        if (fillFraction > 0f) {
            drawRoundRect(fillColor, Offset(0f, barTop), Size(size.width * fillFraction, barHeight), barRadius)
        }
        val tickWidth = BEST_TICK_WIDTH.toPx()
        val tickX = (size.width * tickFraction - tickWidth / 2f).coerceIn(0f, size.width - tickWidth)
        drawRoundRect(tickColor, Offset(tickX, 0f), Size(tickWidth, size.height), CornerRadius(tickWidth / 2f))
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MetaDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
