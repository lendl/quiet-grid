package com.quietgrid.app.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.LogoGridLoader
import com.quietgrid.app.ui.components.pressScale

internal val PLAY_RAIL_WIDTH = 56.dp
private const val COUNTDOWN_WARNING_SECONDS = 10

internal class PlayChromeActions(
    val timer: PlayTimer?,
    val hint: PlayHintAction?,
    val rulesOpen: Boolean,
    val onBack: () -> Unit,
    val onToggleRules: () -> Unit,
    val onEndPuzzle: () -> Unit,
)

@Composable
internal fun PlayTopBar(metrics: PlayMetrics, actions: PlayChromeActions) {
    Box(Modifier.fillMaxWidth().height(metrics.topBarHeight).padding(horizontal = 4.dp)) {
        if (actions.timer != null) {
            PlayTimerText(actions.timer, metrics, Modifier.align(Alignment.Center))
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            PlayBackButton(metrics, actions.onBack)
            Spacer(Modifier.weight(1f))
            PlayTrailingActions(metrics, actions)
        }
    }
}

@Composable
internal fun PlayRail(metrics: PlayMetrics, actions: PlayChromeActions) {
    Column(
        Modifier.width(PLAY_RAIL_WIDTH).fillMaxHeight().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayBackButton(metrics, actions.onBack)
        if (actions.timer != null) {
            PlayTimerText(actions.timer, metrics, Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        PlayTrailingActions(metrics, actions)
    }
}

@Composable
private fun PlayTrailingActions(metrics: PlayMetrics, actions: PlayChromeActions) {
    val hint = actions.hint
    if (hint != null) {
        PlayToggleIconButton(
            Icons.Outlined.Lightbulb,
            hint.contentDescription,
            active = hint.active,
            busy = hint.busy,
            enabled = hint.enabled,
            metrics = metrics,
            onClick = hint.onClick,
        )
    }
    PlayToggleIconButton(
        Icons.AutoMirrored.Outlined.HelpOutline,
        stringResource(R.string.common_rules),
        active = actions.rulesOpen,
        metrics = metrics,
        onClick = actions.onToggleRules,
    )
    PlayOverflowMenu(metrics, actions.onEndPuzzle)
}

@Composable
private fun PlayBackButton(metrics: PlayMetrics, onBack: () -> Unit) {
    PlayToggleIconButton(
        Icons.AutoMirrored.Filled.ArrowBack,
        stringResource(R.string.common_back),
        active = false,
        metrics = metrics,
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onBack,
    )
}

@Composable
private fun PlayTimerText(timer: PlayTimer, metrics: PlayMetrics, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.timerTextSize, fontFeatureSettings = "tnum")
    when (timer) {
        is PlayTimer.Elapsed -> ElapsedTimerText(
            timer.seconds,
            modifier = modifier,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is PlayTimer.Countdown -> CountdownTimerText(timer.seconds, style, modifier)
    }
}

@Composable
private fun CountdownTimerText(seconds: Int, style: TextStyle, modifier: Modifier) {
    val remaining = maxOf(0, seconds)
    val lowTime = remaining <= COUNTDOWN_WARNING_SECONDS
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(remaining) {
        if (lowTime && remaining > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 300
                    1f at 0
                    1.35f at 120
                    1f at 300
                },
            )
        }
    }
    Text(
        formatElapsed(remaining),
        style = style,
        color = if (lowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.graphicsLayer(scaleX = pulse.value, scaleY = pulse.value),
    )
}

@Composable
private fun PlayToggleIconButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    metrics: PlayMetrics,
    busy: Boolean = false,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled && !busy,
        interactionSource = interactionSource,
        modifier = Modifier.size(metrics.iconButtonSize).pressScale(interactionSource),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
    ) {
        if (busy) {
            LogoGridLoader(size = 20.dp)
        } else {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(metrics.iconSize),
                tint = when {
                    !enabled -> tint.copy(alpha = 0.38f)
                    active -> MaterialTheme.colorScheme.primary
                    else -> tint
                },
            )
        }
    }
}

@Composable
private fun PlayOverflowMenu(metrics: PlayMetrics, onEndPuzzle: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PlayToggleIconButton(
            Icons.Filled.MoreVert,
            stringResource(R.string.result_more_options),
            active = false,
            metrics = metrics,
        ) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_end_puzzle)) },
                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEndPuzzle()
                },
            )
        }
    }
}
