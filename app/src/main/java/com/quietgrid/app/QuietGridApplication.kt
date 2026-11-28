package com.quietgrid.app

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.quietgrid.app.notifications.QuietGridWorkerFactory
import com.quietgrid.app.platform.AppLocale

class QuietGridApplication : Application(), Configuration.Provider {
    val graph: AppGraph by lazy { AppGraph(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(QuietGridWorkerFactory(graph)).build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        graph.dailyReminderScheduler.start()
        graph.quickPlayWidgetUpdater.start()
    }
}
