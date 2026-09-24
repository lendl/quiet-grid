package com.quietgrid.app

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.AppLocale
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

    private fun launchThemeFor(mode: ThemeMode): Int = when (mode) {
        ThemeMode.SYSTEM -> Resources.ID_NULL
        ThemeMode.LIGHT -> R.style.Theme_QuietGrid_Launch_Light
        ThemeMode.DARK -> R.style.Theme_QuietGrid_Launch_Dark
        ThemeMode.PENCIL -> R.style.Theme_QuietGrid_Launch_Pencil
        ThemeMode.SILK -> R.style.Theme_QuietGrid_Launch_Silk
        ThemeMode.NORD -> R.style.Theme_QuietGrid_Launch_Nord
        ThemeMode.COFFEE -> R.style.Theme_QuietGrid_Launch_Coffee
        ThemeMode.DRACULA -> R.style.Theme_QuietGrid_Launch_Dracula
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
            val loadedSettings by repositories.settingsRepository.settings.collectAsState(initial = null)
            val settings = loadedSettings ?: return@setContent
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

            LaunchedEffect(settings.themeMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    splashScreen.setSplashScreenTheme(launchThemeFor(settings.themeMode))
                }
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
                val windowBackground = MaterialTheme.colorScheme.background.toArgb()
                LaunchedEffect(windowBackground) {
                    window.setBackgroundDrawable(ColorDrawable(windowBackground))
                }
                AppNavHost(
                    openDailyTab = openDailyTab.value,
                    onOpenDailyTabHandled = { openDailyTab.value = false },
                )
            }
        }
    }
}
