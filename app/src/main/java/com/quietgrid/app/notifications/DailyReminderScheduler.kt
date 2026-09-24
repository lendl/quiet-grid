package com.quietgrid.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.quietgrid.app.core.daily.nextReminderDelay
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_NAME = "daily_reminder"

@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dailyRepository: DailyRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(settingsRepository.settings, dailyRepository.subscribedGames) { settings, subscribed ->
                Triple(settings.dailyReminderEnabled, settings.dailyReminderTime, subscribed.isNotEmpty())
            }.distinctUntilChanged().collect { (enabled, time, hasSubscriptions) -> apply(enabled, time, hasSubscriptions) }
        }
    }

    suspend fun sync() {
        val settings = settingsRepository.settings.first()
        val subscribed = dailyRepository.subscribedGames.first()
        apply(settings.dailyReminderEnabled, settings.dailyReminderTime, subscribed.isNotEmpty())
    }

    private fun apply(enabled: Boolean, time: LocalTime, hasSubscriptions: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled || !hasSubscriptions) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = nextReminderDelay(ZonedDateTime.now(), time)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
