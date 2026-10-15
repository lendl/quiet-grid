package com.quietgrid.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickPlayWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val source: QuickPlaySource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            source.render.collect { updateWidgets() }
        }
    }

    fun refresh() {
        scope.launch { updateWidgets() }
    }

    private suspend fun updateWidgets() {
        QuickPlayWidget().updateAll(context)
        MixWidget().updateAll(context)
    }
}
