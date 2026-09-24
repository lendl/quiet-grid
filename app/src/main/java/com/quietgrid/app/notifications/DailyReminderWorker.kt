package com.quietgrid.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quietgrid.app.core.daily.DailyPools
import com.quietgrid.app.core.daily.dailyEligibleGames
import com.quietgrid.app.core.daily.gamesNeedingReminder
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
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
                val poolSizes = eligible.associateWith { DailyPools.poolSizes(applicationContext, it, settings.puzzleLanguage) }
                val games = gamesNeedingReminder(
                    subscribed = dailyRepository.subscribedGames.first(),
                    eligible = eligible,
                    poolSizes = poolSizes,
                    records = historyRepository.allRecords().first(),
                    envelope = sessionRepository.activeSession.first(),
                    today = LocalDate.now(),
                )
                if (games.isNotEmpty()) DailyReminderNotifier.post(applicationContext, games)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        scheduler.sync()
        return Result.success()
    }
}
