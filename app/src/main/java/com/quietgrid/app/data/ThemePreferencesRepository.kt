package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.themes.toggleThemeExclusion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private fun key(gameId: GameId, locale: String) = stringSetPreferencesKey("themes_excluded_${gameId.key}_$locale")

    fun excludedThemes(gameId: GameId, locale: String): Flow<Set<String>> =
        dataStore.data.map { it[key(gameId, locale)] ?: emptySet() }

    suspend fun setExcludedThemes(gameId: GameId, locale: String, excluded: Set<String>) {
        dataStore.edit { prefs ->
            if (excluded.isEmpty()) prefs.remove(key(gameId, locale)) else prefs[key(gameId, locale)] = excluded
        }
    }

    suspend fun toggleExcludedTheme(gameId: GameId, locale: String, themeId: String, allThemeIds: Set<String>) {
        val prefsKey = key(gameId, locale)
        dataStore.edit { prefs ->
            val current = prefs[prefsKey] ?: emptySet()
            val next = toggleThemeExclusion(current, themeId, allThemeIds)
            if (next.isEmpty()) prefs.remove(prefsKey) else prefs[prefsKey] = next
        }
    }
}
