package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ScreenRotation { AUTO, PORTRAIT, LANDSCAPE }

enum class GameToggle(val keySuffix: String) {
    TIMER("timer"),
    HINTS("hints"),
    KEEP_SCREEN_ON("keep_screen_on"),
    HAPTICS("haptics"),
    MISTAKES("mistakes"),
    REDUCE_MOTION("reduce_motion"),
    LEFT_HANDED("left_handed"),
    PINCH_ZOOM("pinch_zoom"),
    CONFIRM_END("confirm_end"),
    REMEMBER_DIFFICULTY("remember_difficulty"),
}

data class GamePreferenceOverrides(
    val toggles: Map<GameToggle, Boolean> = emptyMap(),
    val rotation: ScreenRotation? = null,
) {
    val isEmpty: Boolean get() = toggles.isEmpty() && rotation == null
}

data class GamePlayPreferences(
    val showTimer: Boolean = true,
    val showHints: Boolean = true,
    val keepScreenOn: Boolean = false,
    val haptics: Boolean = false,
    val rotation: ScreenRotation = ScreenRotation.AUTO,
    val showMistakes: Boolean = true,
    val reduceMotion: Boolean = false,
    val leftHanded: Boolean = false,
    val pinchZoom: Boolean = true,
    val confirmEndPuzzle: Boolean = true,
    val rememberDifficulty: Boolean = true,
)

fun AppSettings.toggleDefault(toggle: GameToggle): Boolean = when (toggle) {
    GameToggle.TIMER -> showTimerInPlay
    GameToggle.HINTS -> showHintsInPlay
    GameToggle.KEEP_SCREEN_ON -> keepScreenOnInPlay
    GameToggle.HAPTICS -> hapticsEnabled
    GameToggle.MISTAKES -> showMistakesInPlay
    GameToggle.REDUCE_MOTION -> reduceMotionInPlay
    GameToggle.LEFT_HANDED -> leftHandedInPlay
    GameToggle.PINCH_ZOOM -> pinchZoomInPlay
    GameToggle.CONFIRM_END -> confirmEndPuzzle
    GameToggle.REMEMBER_DIFFICULTY -> rememberLastDifficulty
}

fun resolveGamePlayPreferences(settings: AppSettings, overrides: GamePreferenceOverrides): GamePlayPreferences {
    fun toggle(toggle: GameToggle) = overrides.toggles[toggle] ?: settings.toggleDefault(toggle)
    return GamePlayPreferences(
        showTimer = toggle(GameToggle.TIMER),
        showHints = toggle(GameToggle.HINTS),
        keepScreenOn = toggle(GameToggle.KEEP_SCREEN_ON),
        haptics = toggle(GameToggle.HAPTICS),
        rotation = overrides.rotation ?: settings.screenRotation,
        showMistakes = toggle(GameToggle.MISTAKES),
        reduceMotion = toggle(GameToggle.REDUCE_MOTION),
        leftHanded = toggle(GameToggle.LEFT_HANDED),
        pinchZoom = toggle(GameToggle.PINCH_ZOOM),
        confirmEndPuzzle = toggle(GameToggle.CONFIRM_END),
        rememberDifficulty = toggle(GameToggle.REMEMBER_DIFFICULTY),
    )
}

@Singleton
class GamePreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private fun toggleKey(gameId: GameId, toggle: GameToggle) =
        booleanPreferencesKey("game_pref_${gameId.key}_${toggle.keySuffix}")

    private fun rotationKey(gameId: GameId) = stringPreferencesKey("game_pref_${gameId.key}_rotation")

    private fun lastDifficultyKey(gameId: GameId) = stringPreferencesKey("game_last_difficulty_${gameId.key}")

    private fun overridesIn(prefs: Preferences, gameId: GameId) = GamePreferenceOverrides(
        toggles = GameToggle.entries.mapNotNull { toggle -> prefs[toggleKey(gameId, toggle)]?.let { toggle to it } }.toMap(),
        rotation = prefs[rotationKey(gameId)]?.let { runCatching { ScreenRotation.valueOf(it) }.getOrNull() },
    )

    fun overrides(gameId: GameId): Flow<GamePreferenceOverrides> =
        dataStore.data.map { overridesIn(it, gameId) }.distinctUntilChanged()

    val allOverrides: Flow<Map<GameId, GamePreferenceOverrides>> = dataStore.data.map { prefs ->
        GameId.entries.associateWith { overridesIn(prefs, it) }.filterValues { !it.isEmpty }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    suspend fun setToggle(gameId: GameId, toggle: GameToggle, value: Boolean?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(toggleKey(gameId, toggle)) else prefs[toggleKey(gameId, toggle)] = value
        }
    }

    suspend fun setRotation(gameId: GameId, rotation: ScreenRotation?) {
        dataStore.edit { prefs ->
            if (rotation == null) prefs.remove(rotationKey(gameId)) else prefs[rotationKey(gameId)] = rotation.name
        }
    }

    fun lastDifficulty(gameId: GameId): Flow<Difficulty?> = dataStore.data.map { prefs ->
        prefs[lastDifficultyKey(gameId)]?.let { key -> Difficulty.entries.firstOrNull { it.key == key } }
    }.distinctUntilChanged()

    suspend fun setLastDifficulty(gameId: GameId, difficulty: Difficulty) {
        dataStore.edit { it[lastDifficultyKey(gameId)] = difficulty.key }
    }

    suspend fun clear(gameId: GameId) {
        dataStore.edit { prefs ->
            GameToggle.entries.forEach { prefs.remove(toggleKey(gameId, it)) }
            prefs.remove(rotationKey(gameId))
        }
    }
}
