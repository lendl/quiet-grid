package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val DAILY_SUBSCRIPTIONS_KEY = stringSetPreferencesKey("daily_subscriptions")
private const val TIER_SEPARATOR = ":"

@Singleton
class DailyRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val subscriptions: Flow<Map<GameId, Set<Difficulty>>> = dataStore.data.map { prefs ->
        parseSubscriptions(prefs[DAILY_SUBSCRIPTIONS_KEY].orEmpty())
    }

    val subscribedGames: Flow<Set<GameId>> = subscriptions.map { it.keys }

    suspend fun setSubscribed(gameId: GameId, subscribed: Boolean) {
        edit { current -> current + (gameId to if (subscribed) Difficulty.entries.toSet() else emptySet()) }
    }

    suspend fun setSubscribed(gameId: GameId, difficulty: Difficulty, subscribed: Boolean) {
        edit { current ->
            val tiers = current[gameId].orEmpty()
            current + (gameId to if (subscribed) tiers + difficulty else tiers - difficulty)
        }
    }

    private suspend fun edit(transform: (Map<GameId, Set<Difficulty>>) -> Map<GameId, Set<Difficulty>>) {
        dataStore.edit { prefs ->
            val updated = transform(parseSubscriptions(prefs[DAILY_SUBSCRIPTIONS_KEY].orEmpty()))
            prefs[DAILY_SUBSCRIPTIONS_KEY] = updated.flatMapTo(HashSet()) { (gameId, tiers) ->
                tiers.map { "${gameId.key}$TIER_SEPARATOR${it.key}" }
            }
        }
    }
}

private fun parseSubscriptions(entries: Set<String>): Map<GameId, Set<Difficulty>> {
    val result = mutableMapOf<GameId, MutableSet<Difficulty>>()
    entries.forEach { entry ->
        val gameKey = entry.substringBefore(TIER_SEPARATOR)
        val gameId = GameId.entries.firstOrNull { it.key == gameKey } ?: return@forEach
        val tiers = result.getOrPut(gameId) { mutableSetOf() }
        if (TIER_SEPARATOR in entry) {
            val tierKey = entry.substringAfter(TIER_SEPARATOR)
            Difficulty.entries.firstOrNull { it.key == tierKey }?.let(tiers::add)
        } else {
            tiers.addAll(Difficulty.entries)
        }
    }
    return result.filterValues { it.isNotEmpty() }
}
