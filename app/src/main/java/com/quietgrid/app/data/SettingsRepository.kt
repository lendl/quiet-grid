package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK, PENCIL }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showTimerInPlay: Boolean = true,
    val betaGamesEnabled: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_TIMER_IN_PLAY = booleanPreferencesKey("show_timer_in_play")
        val BETA_GAMES_ENABLED = booleanPreferencesKey("beta_games_enabled")
    }

    val settings: Flow<AppSettings> = context.appDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showTimerInPlay = prefs[Keys.SHOW_TIMER_IN_PLAY] ?: true,
            betaGamesEnabled = prefs[Keys.BETA_GAMES_ENABLED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShowTimerInPlay(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_TIMER_IN_PLAY] = enabled }
    }

    suspend fun setBetaGamesEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.BETA_GAMES_ENABLED] = enabled }
    }
}
