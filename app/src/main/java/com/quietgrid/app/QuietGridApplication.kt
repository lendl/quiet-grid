package com.quietgrid.app

import android.app.Application
import android.content.Context
import com.quietgrid.app.core.AppLocale
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuietGridApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }
}
