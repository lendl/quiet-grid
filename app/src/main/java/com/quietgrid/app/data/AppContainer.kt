package com.quietgrid.app.data

import android.content.Context

object AppContainer {
    private lateinit var appContext: Context

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var statsRepository: StatsRepository
        private set
    lateinit var sessionRepository: SessionRepository
        private set

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        settingsRepository = SettingsRepository(appContext)
        statsRepository = StatsRepository(appContext)
        sessionRepository = SessionRepository(appContext)
    }
}
