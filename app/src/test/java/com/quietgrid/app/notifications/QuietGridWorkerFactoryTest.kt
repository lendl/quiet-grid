package com.quietgrid.app.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerParameters
import com.quietgrid.app.appGraph
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuietGridWorkerFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val params = mockk<WorkerParameters>(relaxed = true)

    @Test
    fun `builds the daily reminder worker by its stored class name`() {
        val worker = QuietGridWorkerFactory(context.appGraph).createWorker(context, DailyReminderWorker::class.java.name, params)
        assertTrue(worker is DailyReminderWorker)
    }

    @Test
    fun `leaves unknown workers to the default factory`() {
        assertNull(QuietGridWorkerFactory(context.appGraph).createWorker(context, "com.quietgrid.app.UnknownWorker", params))
    }
}
