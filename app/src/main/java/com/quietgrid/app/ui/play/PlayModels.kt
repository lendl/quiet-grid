package com.quietgrid.app.ui.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.quietgrid.app.core.GameId

data class PlayContext(val dailyKey: String? = null, val fromMix: Boolean = false)

val LocalPlayContext = compositionLocalOf { PlayContext() }

val LocalOpenGameRules = compositionLocalOf<((GameId) -> Unit)?> { null }

internal val LocalPlayMetrics = compositionLocalOf { playMetrics(PlaySizeClass.Medium) }

sealed interface PlayTimer {
    data class Elapsed(val seconds: Int) : PlayTimer
    data class Countdown(val seconds: Int) : PlayTimer
}

sealed interface InfoItem {
    data class Label(val text: String, val emphasized: Boolean = false) : InfoItem
    class Custom(val content: @Composable () -> Unit) : InfoItem
}

enum class DockRole { Controls, DragSource }

data class PlayHintMessage(val title: String, val body: String)

data class PlayHintAction(
    val active: Boolean,
    val busy: Boolean,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
