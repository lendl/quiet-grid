package com.quietgrid.app.notifications

import com.quietgrid.app.appGraph
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quietgrid.app.core.daily.DailyPools
import com.quietgrid.app.core.daily.dailyEligibleGames
import com.quietgrid.app.core.daily.gamesNeedingReminder
import com.quietgrid.app.core.isHalloween
import com.quietgrid.app.core.today
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val dailyRepository: DailyRepository,
    private val historyRepository: PlayHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val scheduler: DailyReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            val settings = settingsRepository.settings.first()
            if (settings.dailyReminderEnabled) {
                val eligible = dailyEligibleGames()
                val poolSizes = eligible.associateWith { DailyPools.poolSizes(applicationContext.appGraph.bundledFiles, it, settings.puzzleLanguage) }
                val games = gamesNeedingReminder(
                    subscribed = dailyRepository.subscriptions.first(),
                    eligible = eligible,
                    poolSizes = poolSizes,
                    records = historyRepository.allRecords().first(),
                    envelope = sessionRepository.activeSession.first(),
                    today = today(),
                )
                if (games.isNotEmpty()) {
                    val halloween = settings.seasonalTouchesEnabled && isHalloween(today())
                    DailyReminderNotifier.post(applicationContext, games, halloween)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        scheduler.sync()
        return Result.success()
    }
}
