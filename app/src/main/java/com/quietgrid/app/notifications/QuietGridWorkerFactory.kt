package com.quietgrid.app.notifications

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.quietgrid.app.AppGraph

class QuietGridWorkerFactory(private val graph: AppGraph) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        DailyReminderWorker::class.java.name -> DailyReminderWorker(
            appContext,
            workerParameters,
            graph.settingsRepository,
            graph.dailyRepository,
            graph.playHistoryRepository,
            graph.sessionRepository,
            graph.dailyReminderScheduler,
        )
        else -> null
    }
}
