package com.quietgrid.app.ui.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.quietgrid.app.data.GamePlayPreferences
import com.quietgrid.app.ui.components.LocalHapticsEnabled
import com.quietgrid.app.ui.components.LocalPinchZoomEnabled
import com.quietgrid.app.ui.components.LocalReduceMotion

val LocalGamePlayPreferences = compositionLocalOf { GamePlayPreferences() }

@Composable
fun ProvideGamePlayPreferences(preferences: GamePlayPreferences, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGamePlayPreferences provides preferences,
        LocalHapticsEnabled provides preferences.haptics,
        LocalReduceMotion provides preferences.reduceMotion,
        LocalPinchZoomEnabled provides preferences.pinchZoom,
        content = content,
    )
}
