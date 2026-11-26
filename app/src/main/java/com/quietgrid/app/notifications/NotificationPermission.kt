package com.quietgrid.app.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.LocalAppGraph
import com.quietgrid.app.data.AppSettings
import kotlinx.coroutines.launch

@Composable
fun rememberFirstSubscribePermissionRequest(): () -> Unit {
    val graph = LocalAppGraph.current
    val settings by graph.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings(dailyReminderPermissionAsked = true))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !settings.dailyReminderPermissionAsked &&
            !DailyReminderNotifier.hasPermission(context)
        ) {
            scope.launch { graph.settingsRepository.markDailyReminderPermissionAsked() }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
