package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val DAILY_SUBSCRIPTIONS_KEY = stringSetPreferencesKey("daily_subscriptions")

@Singleton
class DailyRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val subscribedGames: Flow<Set<GameId>> = dataStore.data.map { prefs ->
        val keys = prefs[DAILY_SUBSCRIPTIONS_KEY].orEmpty()
        GameId.entries.filter { it.key in keys }.toSet()
    }

    suspend fun setSubscribed(gameId: GameId, subscribed: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[DAILY_SUBSCRIPTIONS_KEY].orEmpty()
            prefs[DAILY_SUBSCRIPTIONS_KEY] = if (subscribed) current + gameId.key else current - gameId.key
        }
    }
}
