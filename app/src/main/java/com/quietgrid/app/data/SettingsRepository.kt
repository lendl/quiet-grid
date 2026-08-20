package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK, PENCIL }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showTimerInPlay: Boolean = true,
    val betaGamesEnabled: Boolean = false,
    val puzzleLanguage: String = "",
    val quickStartSeenGameIds: Set<String> = emptySet(),
)

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_TIMER_IN_PLAY = booleanPreferencesKey("show_timer_in_play")
        val BETA_GAMES_ENABLED = booleanPreferencesKey("beta_games_enabled")
        val PUZZLE_LANGUAGE = stringPreferencesKey("puzzle_language")
        val QUICK_START_SEEN = stringSetPreferencesKey("quick_start_seen_game_ids")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showTimerInPlay = prefs[Keys.SHOW_TIMER_IN_PLAY] ?: true,
            betaGamesEnabled = prefs[Keys.BETA_GAMES_ENABLED] ?: false,
            puzzleLanguage = prefs[Keys.PUZZLE_LANGUAGE] ?: "",
            quickStartSeenGameIds = prefs[Keys.QUICK_START_SEEN] ?: emptySet(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShowTimerInPlay(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_TIMER_IN_PLAY] = enabled }
    }

    suspend fun setBetaGamesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BETA_GAMES_ENABLED] = enabled }
    }

    suspend fun setPuzzleLanguage(languageTag: String) {
        dataStore.edit { it[Keys.PUZZLE_LANGUAGE] = languageTag }
    }

    suspend fun markQuickStartSeen(gameId: GameId) {
        dataStore.edit { it[Keys.QUICK_START_SEEN] = (it[Keys.QUICK_START_SEEN] ?: emptySet()) + gameId.key }
    }
}
