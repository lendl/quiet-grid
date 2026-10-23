package com.quietgrid.app.ui.play

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import com.quietgrid.app.data.ScreenRotation

@Composable
internal fun ApplyPlayOrientation(rotation: ScreenRotation, phonePortraitOnly: Boolean) {
    val activity = LocalActivity.current ?: return
    val isPhone = isPhoneDevice(LocalConfiguration.current.smallestScreenWidthDp)
    val requested = when {
        phonePortraitOnly && isPhone -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        rotation == ScreenRotation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        rotation == ScreenRotation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        else -> null
    }
    DisposableEffect(activity, requested) {
        if (requested == null) return@DisposableEffect onDispose {}
        val previous = activity.requestedOrientation
        activity.requestedOrientation = requested
        onDispose { activity.requestedOrientation = previous }
    }
}
