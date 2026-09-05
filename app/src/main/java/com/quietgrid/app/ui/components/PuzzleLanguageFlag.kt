package com.quietgrid.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.quietgrid.app.core.localeFlagEmoji

@Composable
fun PuzzleLanguageFlag(puzzleLocale: String, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val appLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
        ?: ConfigurationCompat.getLocales(configuration).get(0)?.language
    if (puzzleLocale.isNotEmpty() && puzzleLocale != appLanguage) {
        Text(
            localeFlagEmoji(puzzleLocale),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(horizontal = 4.dp),
        )
    }
}
