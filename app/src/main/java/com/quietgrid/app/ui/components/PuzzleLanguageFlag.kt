package com.quietgrid.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.localeFlagEmoji
import java.util.Locale

@Composable
fun PuzzleLanguageFlag(puzzleLocale: String, modifier: Modifier = Modifier) {
    val appLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    if (puzzleLocale.isNotEmpty() && puzzleLocale != appLanguage) {
        Text(
            localeFlagEmoji(puzzleLocale),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier.padding(horizontal = 4.dp),
        )
    }
}
