package com.quietgrid.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.data.AppSettings

/** Renders the elapsed-time label, or nothing when the user has disabled it in Settings. */
@Composable
fun ElapsedTimerText(elapsedSeconds: Int, modifier: Modifier = Modifier) {
    val settings by AppContainer.settingsRepository.settings.collectAsState(initial = AppSettings())
    if (settings.showTimerInPlay) {
        Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}
