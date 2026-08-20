package com.quietgrid.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel

@Composable
fun ElapsedTimerText(elapsedSeconds: Int, modifier: Modifier = Modifier) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    if (settings.showTimerInPlay) {
        Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}
