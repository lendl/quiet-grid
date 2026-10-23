package com.quietgrid.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

val LocalReduceMotion = compositionLocalOf { false }

fun systemAnimationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val systemDisabled = remember(context) { systemAnimationsDisabled(context) }
    return LocalReduceMotion.current || systemDisabled
}
