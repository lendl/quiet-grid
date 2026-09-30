package com.quietgrid.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel

@Composable
fun ElapsedTimerText(
    elapsedSeconds: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    if (settings.showTimerInPlay) {
        Text(formatElapsed(elapsedSeconds), style = style, color = color, modifier = modifier)
    }
}
