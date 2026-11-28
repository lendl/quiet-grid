package com.quietgrid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.quietgrid.app.platform.AndroidHaptics
import com.quietgrid.app.platform.Haptics

val LocalHapticsEnabled = compositionLocalOf { false }

@Composable
fun rememberHapticController(): Haptics {
    val context = LocalContext.current
    val hapticsEnabled = LocalHapticsEnabled.current
    return remember(context, hapticsEnabled) { AndroidHaptics(context, hapticsEnabled) }
}
