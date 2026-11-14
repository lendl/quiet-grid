package com.quietgrid.app.ui.play

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ScreenRotation

interface QuietTipSignals {
    fun rotationBounce(gameId: GameId, target: ScreenRotation)
    fun screenTimeout(gameId: GameId)
}

val LocalQuietTipSignals = staticCompositionLocalOf<QuietTipSignals?> { null }

private const val BOUNCE_WINDOW_MS = 8_000L
private const val DEFAULT_SCREEN_OFF_TIMEOUT_MS = 30_000
private const val TIMEOUT_SLACK_MS = 2_000L
private const val PAUSE_GRACE_MS = 3_000L

internal class PlayTouches {
    var lastTouchMillis: Long = SystemClock.elapsedRealtime()
    var boardTouchedSinceTurn: Boolean = false
}

private class RotationWatch(var lastOrientation: Int) {
    var turnStart: Int? = null
    var turnedAtMillis: Long = 0L
}

internal fun Modifier.observeScreenTouches(touches: PlayTouches): Modifier = pointerInput(touches) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            touches.lastTouchMillis = SystemClock.elapsedRealtime()
        }
    }
}

internal fun Modifier.observeBoardTouches(touches: PlayTouches): Modifier = pointerInput(touches) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            touches.boardTouchedSinceTurn = true
        }
    }
}

@Composable
internal fun trackQuietTipSignals(
    gameId: GameId,
    rotation: ScreenRotation,
    keepsScreenOn: Boolean,
    phonePortraitOnly: Boolean,
): PlayTouches {
    val touches = remember { PlayTouches() }
    val signals = LocalQuietTipSignals.current
    if (signals != null) {
        val configuration = LocalConfiguration.current
        val rotationLocked = phonePortraitOnly && isPhoneDevice(configuration.smallestScreenWidthDp)
        TrackRotationBounces(gameId, touches, signals, active = rotation == ScreenRotation.AUTO && !rotationLocked)
        if (!keepsScreenOn) TrackScreenTimeouts(gameId, touches, signals)
    }
    return touches
}

@Composable
private fun TrackRotationBounces(gameId: GameId, touches: PlayTouches, signals: QuietTipSignals, active: Boolean) {
    val orientation = LocalConfiguration.current.orientation
    val watch = remember { RotationWatch(orientation) }
    LaunchedEffect(orientation, active) {
        val previous = watch.lastOrientation
        watch.lastOrientation = orientation
        if (!active || previous == Configuration.ORIENTATION_UNDEFINED || orientation == Configuration.ORIENTATION_UNDEFINED) {
            watch.turnStart = null
            return@LaunchedEffect
        }
        if (orientation == previous) return@LaunchedEffect
        val now = SystemClock.elapsedRealtime()
        val start = watch.turnStart
        if (start == orientation && (now - watch.turnedAtMillis < BOUNCE_WINDOW_MS || !touches.boardTouchedSinceTurn)) {
            val target = if (start == Configuration.ORIENTATION_PORTRAIT) ScreenRotation.PORTRAIT else ScreenRotation.LANDSCAPE
            signals.rotationBounce(gameId, target)
            watch.turnStart = null
            return@LaunchedEffect
        }
        watch.turnStart = previous
        watch.turnedAtMillis = now
        touches.boardTouchedSinceTurn = false
    }
}

@Composable
private fun TrackScreenTimeouts(gameId: GameId, touches: PlayTouches, signals: QuietTipSignals) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, gameId, signals, touches) {
        var pausedAtMillis = 0L
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) pausedAtMillis = SystemClock.elapsedRealtime()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val now = SystemClock.elapsedRealtime()
                val showing = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || now - pausedAtMillis < PAUSE_GRACE_MS
                if (!showing) return
                val timeout = Settings.System.getInt(receiverContext.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_SCREEN_OFF_TIMEOUT_MS)
                if (now - touches.lastTouchMillis >= timeout - TIMEOUT_SLACK_MS) signals.screenTimeout(gameId)
            }
        }
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycle.removeObserver(observer)
        }
    }
}
