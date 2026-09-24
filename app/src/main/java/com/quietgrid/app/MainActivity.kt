package com.quietgrid.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.AppLocale
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.nav.AppNavHost
import com.quietgrid.app.notifications.OPEN_TAB_DAILY
import com.quietgrid.app.notifications.OPEN_TAB_EXTRA
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openDailyTab = mutableStateOf(false)

    private fun Intent?.opensDailyTab(): Boolean = this?.getStringExtra(OPEN_TAB_EXTRA) == OPEN_TAB_DAILY

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.opensDailyTab()) openDailyTab.value = true
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.opensDailyTab()) openDailyTab.value = true
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
                ThemeMode.SILK -> ResolvedTheme.SILK
                ThemeMode.NORD -> ResolvedTheme.NORD
                ThemeMode.COFFEE -> ResolvedTheme.COFFEE
                ThemeMode.DRACULA -> ResolvedTheme.DRACULA
            }

            LaunchedEffect(resolvedTheme) {
                val useDarkIcons = resolvedTheme != ResolvedTheme.DARK &&
                    resolvedTheme != ResolvedTheme.COFFEE &&
                    resolvedTheme != ResolvedTheme.DRACULA
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = useDarkIcons
                    isAppearanceLightNavigationBars = useDarkIcons
                }
            }

            QuietGridTheme(resolvedTheme = resolvedTheme) {
                AppNavHost(
                    openDailyTab = openDailyTab.value,
                    onOpenDailyTabHandled = { openDailyTab.value = false },
                )
            }
        }
    }
}
