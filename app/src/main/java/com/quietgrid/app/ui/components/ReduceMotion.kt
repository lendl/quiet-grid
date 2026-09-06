package com.quietgrid.app.ui.components

import android.content.Context
import android.provider.Settings

fun systemAnimationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
