package com.quietgrid.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.nav.AppNavHost
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not plain ComponentActivity) is required for AppCompatDelegate's per-app
// language switching (Settings > Language) to actually recreate the activity with the new
// locale on API levels before 33 — without it, setApplicationLocales() silently no-ops.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default enableEdgeToEdge() still paints a translucent scrim behind 3-button/legacy nav
        // bars so system icons stay legible over arbitrary content — that scrim doesn't match our
        // own bottom bar color, showing up as a visibly different band. Force it fully transparent
        // so our own Compose content's background shows through cleanly instead.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            val repositories: RepositoriesViewModel = hiltViewModel()
            val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            val resolvedTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> if (systemDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
                ThemeMode.LIGHT -> ResolvedTheme.LIGHT
                ThemeMode.DARK -> ResolvedTheme.DARK
                ThemeMode.PENCIL -> ResolvedTheme.PENCIL
            }

            QuietGridTheme(resolvedTheme = resolvedTheme) {
                AppNavHost()
            }
        }
    }
}
