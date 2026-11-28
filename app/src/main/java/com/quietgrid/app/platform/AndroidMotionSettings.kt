package com.quietgrid.app.platform

import android.content.Context
import android.provider.Settings

class AndroidMotionSettings(private val context: Context) : MotionSettings {
    override fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}
