package com.quietgrid.app.ui.journey

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.ui.components.rememberReduceMotion

private const val GOAL_BRIGHTEN_MS = 250f
private const val GOAL_DIM_ALPHA = 0.4f

@Composable
fun JourneyResultScreen(
    gameId: GameId,
    stopId: String,
    elapsedSeconds: Int,
    signals: JourneySignals,
    onPlay: (Difficulty, JourneyRequest) -> Unit,
    onMap: () -> Unit,
) {
    val viewModel: JourneyViewModel = hiltViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsState(initial = null)
    val rows = state?.rows ?: return
    val model = remember(rows, stopId, elapsedSeconds, signals) { journeyResultModel(rows, stopId, elapsedSeconds, signals) }
    if (model == null) {
        LaunchedEffect(Unit) { onMap() }
        return
    }
    fun play(row: JourneyStopRow) = onPlay(Difficulty.fromKey(row.stop.difficulty), journeyRequestFor(row))
    JourneyResultContent(
        gameId = gameId,
        model = model,
        reduceMotion = rememberReduceMotion(),
        onNext = { model.next?.let(::play) },
        onReplay = { model.replay?.let(::play) },
        onMap = onMap,
    )
}

@Composable
fun JourneyResultContent(
    gameId: GameId,
    model: JourneyResultModel,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onMap: () -> Unit,
) {
    val totalMs = gentlePopTotalMs(model.runStars)
    var revealStarted by rememberSaveable { mutableStateOf(false) }
    val clock = remember { Animatable(if (reduceMotion || revealStarted) totalMs.toFloat() else 0f) }
    LaunchedEffect(Unit) {
        if (revealStarted) return@LaunchedEffect
        revealStarted = true
        if (!reduceMotion) clock.animateTo(totalMs.toFloat(), tween(totalMs.toInt(), easing = LinearEasing))
    }
    val elapsedMs = clock.value.toLong()
    val brighten = goalBrightenMs(model.goals)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                stringResource(R.string.journey_card_position, model.chapterIndex, model.stopNumber),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.journey_board_cleared),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    formatElapsed(model.elapsedSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StarReveal(model.runStars, elapsedMs)
                if (model.bestStars > model.runStars) {
                    Text(
                        stringResource(R.string.journey_best_stars, model.bestStars),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                model.goals.forEachIndexed { index, goal ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val shown = ((elapsedMs - brighten[index]) / GOAL_BRIGHTEN_MS).coerceIn(0f, 1f)
                    JourneyGoalRow(model, goal, Modifier.alpha(GOAL_DIM_ALPHA + (1f - GOAL_DIM_ALPHA) * shown))
                }
            }
            Spacer(Modifier.heightIn(min = 8.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (model.next != null) {
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                        Text(stringResource(R.string.journey_next_stop), fontWeight = FontWeight.Bold)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp).size(18.dp),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (model.replay != null) {
                        OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                            Text(stringResource(R.string.journey_replay_stars))
                        }
                    }
                    OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
                        Text(stringResource(R.string.journey_map))
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyGoalRow(model: JourneyResultModel, goal: JourneyGoal, modifier: Modifier) {
    val label = when (goal.kind) {
        JourneyGoalKind.CLEAR -> stringResource(R.string.journey_goal_clear)
        JourneyGoalKind.CLEAN -> stringResource(R.string.journey_goal_clean)
        JourneyGoalKind.TIME -> stringResource(R.string.journey_goal_time, formatElapsed(model.targetSeconds))
    }
    val hints = model.signals.hintsUsed
    val mistakes = model.signals.mistakes
    val detail = when (goal.kind) {
        JourneyGoalKind.CLEAR -> null
        JourneyGoalKind.CLEAN -> if (goal.met) {
            null
        } else {
            listOfNotNull(
                if (hints > 0) pluralStringResource(R.plurals.journey_hints, hints, hints) else null,
                if (mistakes > 0) pluralStringResource(R.plurals.journey_mistakes, mistakes, mistakes) else null,
            ).joinToString(" " + stringResource(R.string.play_info_separator) + " ")
        }
        JourneyGoalKind.TIME -> formatElapsed(model.elapsedSeconds)
    }
    val tint = if (goal.met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val statusDescription = stringResource(if (goal.met) R.string.journey_goal_met else R.string.journey_goal_missed)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GoalMark(goal.met, tint, Modifier.size(22.dp).semantics { contentDescription = statusDescription })
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (goal.met) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (!detail.isNullOrEmpty()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalMark(met: Boolean, tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        drawCircle(tint, radius = 9f * unit, style = Stroke(width = 2.2f * unit))
        if (met) {
            val stroke = 2.4f * unit
            drawLine(tint, Offset(8f * unit, 12.5f * unit), Offset(10.5f * unit, 15f * unit), stroke, StrokeCap.Round)
            drawLine(tint, Offset(10.5f * unit, 15f * unit), Offset(16f * unit, 9.5f * unit), stroke, StrokeCap.Round)
        } else {
            drawLine(tint, Offset(8.5f * unit, 12f * unit), Offset(15.5f * unit, 12f * unit), 2.2f * unit, StrokeCap.Round)
        }
    }
}
