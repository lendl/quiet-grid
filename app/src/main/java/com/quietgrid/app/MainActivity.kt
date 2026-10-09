package com.quietgrid.app

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.AppLocale
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.nav.AppNavHost
import com.quietgrid.app.ui.components.LocalHapticsEnabled
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.fixedResolvedTheme
import com.quietgrid.app.widget.QuickPlayWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var quickPlayWidgetUpdater: QuickPlayWidgetUpdater
    private val launchRequest = mutableStateOf<LaunchRequest?>(null)
    private var firstFrameReady = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toLaunchRequest()?.let { launchRequest.value = it }
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
        if (savedInstanceState == null) launchRequest.value = intent.toLaunchRequest()
        quickPlayWidgetUpdater.refresh()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        holdFirstDrawUntilReady()

        setContent {
            val repositories: RepositoriesViewModel = hiltViewModel()
            val loadedSettings by repositories.settingsRepository.settings.collectAsState(initial = null)
            val settings = loadedSettings ?: return@setContent
            val systemDark = isSystemInDarkTheme()
            val resolvedTheme = settings.themeMode.fixedResolvedTheme()
                ?: if (systemDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT

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
                SideEffect {
                    window.setBackgroundDrawable(ColorDrawable(windowBackground))
                    firstFrameReady = true
                }
                CompositionLocalProvider(LocalHapticsEnabled provides settings.hapticsEnabled) {
                    AppNavHost(
                        launchRequest = launchRequest.value,
                        onLaunchRequestHandled = { launchRequest.value = null },
                    )
                }
            }
        }
    }
    private fun holdFirstDrawUntilReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!firstFrameReady) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
