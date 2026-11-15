package com.quietgrid.app.ui.journey

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

data class CelebrationStat(val value: String, val label: String, val spokenValue: String = value, val starTint: Color? = null)

@Composable
fun rememberCelebrationClock(totalMs: Long, reduceMotion: Boolean): () -> Long {
    var started by rememberSaveable { mutableStateOf(false) }
    val clock = remember { Animatable(if (reduceMotion || started) totalMs.toFloat() else 0f) }
    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        started = true
        if (!reduceMotion) clock.animateTo(totalMs.toFloat(), tween(totalMs.toInt(), easing = LinearEasing))
    }
    return remember(clock) { { clock.value.toLong() } }
}

fun Modifier.rise(
    elapsedMs: () -> Long,
    startMs: Long,
    durationMs: Long = CELEBRATION_RISE_MS,
    distanceDp: Float = CELEBRATION_RISE_DP,
): Modifier = graphicsLayer {
    val frame = riseFrame(elapsedMs(), startMs, durationMs, distanceDp)
    alpha = frame.alpha
    translationY = frame.offsetDp.dp.toPx()
}

@Composable
fun CelebrationScaffold(onClose: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 4.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CelebrationStatsCard(stats: List<CelebrationStat>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 14.dp), color = colors.outlineVariant)
            val spoken = "${stat.spokenValue} ${stat.label}"
            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = spoken },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stat.starTint != null) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = stat.starTint, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                    }
                    Text(
                        stat.value,
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Text(stat.label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
