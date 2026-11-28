package com.quietgrid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.quietgrid.app.platform.MotionSettings

val LocalReduceMotion = compositionLocalOf { false }

val LocalMotionSettings: ProvidableCompositionLocal<MotionSettings> = staticCompositionLocalOf { MotionSettings { true } }

@Composable
fun rememberReduceMotion(): Boolean {
    val motionSettings = LocalMotionSettings.current
    val systemDisabled = remember(motionSettings) { !motionSettings.animationsEnabled() }
    return LocalReduceMotion.current || systemDisabled
}
