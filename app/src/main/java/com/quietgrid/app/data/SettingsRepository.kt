package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK, PENCIL, SILK, NORD, COFFEE, DRACULA }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showTimerInPlay: Boolean = true,
    val showHintsInPlay: Boolean = true,
    val screenRotation: ScreenRotation = ScreenRotation.AUTO,
    val keepScreenOnInPlay: Boolean = false,
    val hapticsEnabled: Boolean = false,
    val showMistakesInPlay: Boolean = true,
    val reduceMotionInPlay: Boolean = false,
    val leftHandedInPlay: Boolean = false,
    val pinchZoomInPlay: Boolean = true,
    val confirmEndPuzzle: Boolean = true,
    val betaGamesEnabled: Boolean = false,
    val puzzleLanguage: String = "",
    val quickStartSeenGameIds: Set<String> = emptySet(),
    val favoriteGameIds: Set<String> = emptySet(),
    val newBadgeSeenGameIds: Set<String> = emptySet(),
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: LocalTime = LocalTime.of(9, 0),
    val dailyReminderPermissionAsked: Boolean = false,
    val seasonalTouchesEnabled: Boolean = true,
)

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_TIMER_IN_PLAY = booleanPreferencesKey("show_timer_in_play")
        val SHOW_HINTS_IN_PLAY = booleanPreferencesKey("show_hints_in_play")
        val SCREEN_ROTATION = stringPreferencesKey("screen_rotation")
        val KEEP_SCREEN_ON_IN_PLAY = booleanPreferencesKey("keep_screen_on_in_play")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_MISTAKES_IN_PLAY = booleanPreferencesKey("show_mistakes_in_play")
        val REDUCE_MOTION_IN_PLAY = booleanPreferencesKey("reduce_motion_in_play")
        val LEFT_HANDED_IN_PLAY = booleanPreferencesKey("left_handed_in_play")
        val PINCH_ZOOM_IN_PLAY = booleanPreferencesKey("pinch_zoom_in_play")
        val CONFIRM_END_PUZZLE = booleanPreferencesKey("confirm_end_puzzle")
        val BETA_GAMES_ENABLED = booleanPreferencesKey("beta_games_enabled")
        val PUZZLE_LANGUAGE = stringPreferencesKey("puzzle_language")
        val QUICK_START_SEEN = stringSetPreferencesKey("quick_start_seen_game_ids")
        val FAVORITE_GAMES = stringSetPreferencesKey("favorite_game_ids")
        val NEW_BADGE_SEEN = stringSetPreferencesKey("new_badge_seen_game_ids")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTE_OF_DAY = intPreferencesKey("daily_reminder_minute_of_day")
        val DAILY_REMINDER_PERMISSION_ASKED = booleanPreferencesKey("daily_reminder_permission_asked")
        val SEASONAL_TOUCHES_ENABLED = booleanPreferencesKey("seasonal_touches_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showTimerInPlay = prefs[Keys.SHOW_TIMER_IN_PLAY] ?: true,
            showHintsInPlay = prefs[Keys.SHOW_HINTS_IN_PLAY] ?: true,
            screenRotation = prefs[Keys.SCREEN_ROTATION]?.let { runCatching { ScreenRotation.valueOf(it) }.getOrNull() }
                ?: ScreenRotation.AUTO,
            keepScreenOnInPlay = prefs[Keys.KEEP_SCREEN_ON_IN_PLAY] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: false,
            showMistakesInPlay = prefs[Keys.SHOW_MISTAKES_IN_PLAY] ?: true,
            reduceMotionInPlay = prefs[Keys.REDUCE_MOTION_IN_PLAY] ?: false,
            leftHandedInPlay = prefs[Keys.LEFT_HANDED_IN_PLAY] ?: false,
            pinchZoomInPlay = prefs[Keys.PINCH_ZOOM_IN_PLAY] ?: true,
            confirmEndPuzzle = prefs[Keys.CONFIRM_END_PUZZLE] ?: true,
            betaGamesEnabled = prefs[Keys.BETA_GAMES_ENABLED] ?: false,
            puzzleLanguage = prefs[Keys.PUZZLE_LANGUAGE] ?: "",
            quickStartSeenGameIds = prefs[Keys.QUICK_START_SEEN] ?: emptySet(),
            favoriteGameIds = prefs[Keys.FAVORITE_GAMES] ?: emptySet(),
            newBadgeSeenGameIds = prefs[Keys.NEW_BADGE_SEEN] ?: emptySet(),
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: true,
            dailyReminderTime = (prefs[Keys.DAILY_REMINDER_MINUTE_OF_DAY] ?: 540).let { LocalTime.of(it / 60, it % 60) },
            dailyReminderPermissionAsked = prefs[Keys.DAILY_REMINDER_PERMISSION_ASKED] ?: false,
            seasonalTouchesEnabled = prefs[Keys.SEASONAL_TOUCHES_ENABLED] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShowTimerInPlay(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_TIMER_IN_PLAY] = enabled }
    }

    suspend fun setShowHintsInPlay(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_HINTS_IN_PLAY] = enabled }
    }

    suspend fun setScreenRotation(rotation: ScreenRotation) {
        dataStore.edit { it[Keys.SCREEN_ROTATION] = rotation.name }
    }

    suspend fun setKeepScreenOnInPlay(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON_IN_PLAY] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setPlayDefault(toggle: GameToggle, enabled: Boolean) {
        val key = when (toggle) {
            GameToggle.TIMER -> Keys.SHOW_TIMER_IN_PLAY
            GameToggle.HINTS -> Keys.SHOW_HINTS_IN_PLAY
            GameToggle.KEEP_SCREEN_ON -> Keys.KEEP_SCREEN_ON_IN_PLAY
            GameToggle.HAPTICS -> Keys.HAPTICS_ENABLED
            GameToggle.MISTAKES -> Keys.SHOW_MISTAKES_IN_PLAY
            GameToggle.REDUCE_MOTION -> Keys.REDUCE_MOTION_IN_PLAY
            GameToggle.LEFT_HANDED -> Keys.LEFT_HANDED_IN_PLAY
            GameToggle.PINCH_ZOOM -> Keys.PINCH_ZOOM_IN_PLAY
            GameToggle.CONFIRM_END -> Keys.CONFIRM_END_PUZZLE
        }
        dataStore.edit { it[key] = enabled }
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

    suspend fun setFavorite(gameId: GameId, favorite: Boolean) {
        dataStore.edit {
            val current = it[Keys.FAVORITE_GAMES] ?: emptySet()
            it[Keys.FAVORITE_GAMES] = if (favorite) current + gameId.key else current - gameId.key
        }
    }

    suspend fun markNewBadgeSeen(gameId: GameId) {
        dataStore.edit { it[Keys.NEW_BADGE_SEEN] = (it[Keys.NEW_BADGE_SEEN] ?: emptySet()) + gameId.key }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DAILY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setDailyReminderTime(time: LocalTime) {
        dataStore.edit { it[Keys.DAILY_REMINDER_MINUTE_OF_DAY] = time.hour * 60 + time.minute }
    }

    suspend fun markDailyReminderPermissionAsked() {
        dataStore.edit { it[Keys.DAILY_REMINDER_PERMISSION_ASKED] = true }
    }

    suspend fun setSeasonalTouchesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SEASONAL_TOUCHES_ENABLED] = enabled }
    }
}
