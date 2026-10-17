package com.quietgrid.app.ui.play

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal fun LockPortraitOnPhones() {
    val activity = LocalActivity.current ?: return
    val isPhone = isPhoneDevice(LocalConfiguration.current.smallestScreenWidthDp)
    DisposableEffect(activity, isPhone) {
        if (!isPhone) return@DisposableEffect onDispose {}
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        onDispose { activity.requestedOrientation = previous }
    }
}
