package com.quietgrid.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.quietgrid.app.data.BadgeRepository
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.GamePreferencesRepository
import com.quietgrid.app.data.JourneyProgressDao
import com.quietgrid.app.data.JourneyProgressRepository
import com.quietgrid.app.data.MIGRATION_2_3
import com.quietgrid.app.data.MIGRATION_3_4
import com.quietgrid.app.data.MIGRATION_4_5
import com.quietgrid.app.data.MIGRATION_5_6
import com.quietgrid.app.data.MIGRATION_6_7
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.PlayHistoryDao
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.QuietGridDatabase
import com.quietgrid.app.data.QuietTipsRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsRepository
import com.quietgrid.app.data.ThemePreferencesRepository
import com.quietgrid.app.data.appDataStore
import com.quietgrid.app.notifications.DailyReminderScheduler
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ProcessAppForeground
import com.quietgrid.app.widget.QuickPlaySource
import com.quietgrid.app.widget.QuickPlayWidgetUpdater

class AppGraph(val appContext: Context) {
    val dataStore: DataStore<Preferences> by lazy { appContext.appDataStore }
    val database: QuietGridDatabase by lazy {
        Room.databaseBuilder(appContext, QuietGridDatabase::class.java, "quiet_grid.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }
    val playHistoryDao: PlayHistoryDao by lazy { database.playHistoryDao() }
    val journeyProgressDao: JourneyProgressDao by lazy { database.journeyProgressDao() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(dataStore) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(dataStore) }
    val playHistoryRepository: PlayHistoryRepository by lazy { PlayHistoryRepository(dataStore, playHistoryDao) }
    val statsRepository: StatsRepository by lazy { StatsRepository(dataStore, playHistoryRepository) }
    val mixRepository: MixRepository by lazy { MixRepository(dataStore) }
    val dailyRepository: DailyRepository by lazy { DailyRepository(dataStore) }
    val themePreferencesRepository: ThemePreferencesRepository by lazy { ThemePreferencesRepository(dataStore) }
    val gamePreferencesRepository: GamePreferencesRepository by lazy { GamePreferencesRepository(dataStore) }
    val quietTipsRepository: QuietTipsRepository by lazy { QuietTipsRepository(dataStore) }
    val journeyProgressRepository: JourneyProgressRepository by lazy {
        JourneyProgressRepository(appContext, journeyProgressDao, playHistoryRepository, settingsRepository)
    }
    val badgeRepository: BadgeRepository by lazy {
        BadgeRepository(appContext, dataStore, playHistoryRepository, journeyProgressRepository, mixRepository, settingsRepository)
    }

    val appForeground: AppForeground by lazy { ProcessAppForeground() }
    val dailyReminderScheduler: DailyReminderScheduler by lazy {
        DailyReminderScheduler(appContext, settingsRepository, dailyRepository)
    }
    val quickPlaySource: QuickPlaySource by lazy {
        QuickPlaySource(sessionRepository, mixRepository, playHistoryRepository, settingsRepository)
    }
    val quickPlayWidgetUpdater: QuickPlayWidgetUpdater by lazy { QuickPlayWidgetUpdater(appContext, quickPlaySource) }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as QuietGridApplication).graph

val LocalAppGraph: ProvidableCompositionLocal<AppGraph> = staticCompositionLocalOf {
    error("LocalAppGraph is not provided; wrap the content in CompositionLocalProvider(LocalAppGraph provides appGraph)")
}

@Composable
inline fun <reified VM : ViewModel> appViewModel(noinline create: CreationExtras.(AppGraph) -> VM): VM {
    val graph = LocalAppGraph.current
    return viewModel { create(graph) }
}
