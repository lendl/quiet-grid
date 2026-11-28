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
import com.quietgrid.app.data.BadgeRepository
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.GamePreferencesRepository
import com.quietgrid.app.data.JourneyProgressDao
import com.quietgrid.app.data.JourneyProgressRepository
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.PlayHistoryDao
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.QuietGridDatabase
import com.quietgrid.app.data.QuietTipsRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsRepository
import com.quietgrid.app.data.ThemePreferencesRepository
import com.quietgrid.app.data.quietGridDataStore
import com.quietgrid.app.data.quietGridDatabase
import com.quietgrid.app.notifications.DailyReminderScheduler
import com.quietgrid.app.platform.AndroidAppInfo
import com.quietgrid.app.platform.AndroidAppLanguage
import com.quietgrid.app.platform.AndroidBundledFiles
import com.quietgrid.app.platform.AndroidMotionSettings
import com.quietgrid.app.platform.AndroidShareSheet
import com.quietgrid.app.platform.AppInfo
import com.quietgrid.app.platform.AppLanguage
import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.MotionSettings
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.platform.ProcessAppForeground
import com.quietgrid.app.platform.ShareSheet
import com.quietgrid.app.widget.QuickPlaySource
import com.quietgrid.app.widget.QuickPlayWidgetUpdater

class AppGraph(val appContext: Context) {
    val dataStore: DataStore<Preferences> by lazy { quietGridDataStore(appContext) }
    val database: QuietGridDatabase by lazy { quietGridDatabase(appContext) }
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
        JourneyProgressRepository(bundledFiles, journeyProgressDao, playHistoryRepository, settingsRepository)
    }
    val badgeRepository: BadgeRepository by lazy {
        BadgeRepository(bundledFiles, dataStore, playHistoryRepository, journeyProgressRepository, mixRepository, settingsRepository)
    }

    val bundledFiles: BundledFiles by lazy { AndroidBundledFiles(appContext) }
    val appLanguage: AppLanguage by lazy { AndroidAppLanguage(appContext) }
    val motionSettings: MotionSettings by lazy { AndroidMotionSettings(appContext) }
    val appInfo: AppInfo by lazy { AndroidAppInfo(appContext) }
    val shareSheet: ShareSheet by lazy { AndroidShareSheet(appContext) }

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
