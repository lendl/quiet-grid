package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.tips.QuietTipState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class QuietTipsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val LAST_SHOWN_DAY = stringPreferencesKey("tip_last_shown_day")
        val RESULTS_SINCE_LAST = intPreferencesKey("tip_results_since_last")
        val SHOWN_COUNTS = stringSetPreferencesKey("tip_shown_counts")
        val CLOSED_KEYS = stringSetPreferencesKey("tip_closed_keys")
        val OPENED_KEYS = stringSetPreferencesKey("tip_opened_keys")
        val SNOOZED_UNTIL = stringSetPreferencesKey("tip_snoozed_until")
        val EARLY_EXITS_IN_ROW = intPreferencesKey("tip_early_exits_in_row")
        val DAILY_TAB_OPENED = booleanPreferencesKey("tip_daily_tab_opened")

        fun rotationBounces(gameId: GameId) = intPreferencesKey("tip_rotation_bounces_${gameId.key}")
        fun rotationTarget(gameId: GameId) = stringPreferencesKey("tip_rotation_target_${gameId.key}")
        fun screenTimeouts(gameId: GameId) = intPreferencesKey("tip_screen_timeouts_${gameId.key}")
    }

    fun state(): Flow<QuietTipState> = dataStore.data.map { prefs ->
        QuietTipState(
            lastShownDay = prefs[Keys.LAST_SHOWN_DAY]?.let(::parseDate),
            resultsSinceLast = prefs[Keys.RESULTS_SINCE_LAST] ?: 0,
            shownCounts = prefs[Keys.SHOWN_COUNTS].orEmpty().decodeEntries { it.toIntOrNull() },
            closedKeys = prefs[Keys.CLOSED_KEYS].orEmpty(),
            openedKeys = prefs[Keys.OPENED_KEYS].orEmpty(),
            snoozedUntil = prefs[Keys.SNOOZED_UNTIL].orEmpty().decodeEntries(::parseDate),
            rotationBounces = perGame { prefs[Keys.rotationBounces(it)] },
            rotationTargets = perGame { gameId ->
                prefs[Keys.rotationTarget(gameId)]?.let { name -> ScreenRotation.entries.firstOrNull { it.name == name } }
            },
            screenTimeouts = perGame { prefs[Keys.screenTimeouts(it)] },
            earlyExitsInRow = prefs[Keys.EARLY_EXITS_IN_ROW] ?: 0,
            dailyTabOpened = prefs[Keys.DAILY_TAB_OPENED] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun recordResult() {
        dataStore.edit { it[Keys.RESULTS_SINCE_LAST] = (it[Keys.RESULTS_SINCE_LAST] ?: 0) + 1 }
    }

    suspend fun recordRotationBounce(gameId: GameId, target: ScreenRotation) {
        dataStore.edit {
            it[Keys.rotationBounces(gameId)] = (it[Keys.rotationBounces(gameId)] ?: 0) + 1
            it[Keys.rotationTarget(gameId)] = target.name
        }
    }

    suspend fun recordScreenTimeout(gameId: GameId) {
        dataStore.edit { it[Keys.screenTimeouts(gameId)] = (it[Keys.screenTimeouts(gameId)] ?: 0) + 1 }
    }

    suspend fun recordResultExit(early: Boolean) {
        dataStore.edit { it[Keys.EARLY_EXITS_IN_ROW] = if (early) (it[Keys.EARLY_EXITS_IN_ROW] ?: 0) + 1 else 0 }
    }

    suspend fun markDailyTabOpened() {
        dataStore.edit { it[Keys.DAILY_TAB_OPENED] = true }
    }

    suspend fun markShown(key: String, today: LocalDate) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SHOWN_DAY] = today.toString()
            prefs[Keys.RESULTS_SINCE_LAST] = 0
            val counts = prefs[Keys.SHOWN_COUNTS].orEmpty().decodeEntries { it.toIntOrNull() }
            prefs[Keys.SHOWN_COUNTS] = (counts + (key to (counts[key] ?: 0) + 1)).encodeEntries()
        }
    }

    suspend fun markOpened(key: String) {
        dataStore.edit { it[Keys.OPENED_KEYS] = it[Keys.OPENED_KEYS].orEmpty() + key }
    }

    suspend fun close(key: String) {
        dataStore.edit { it[Keys.CLOSED_KEYS] = it[Keys.CLOSED_KEYS].orEmpty() + key }
    }

    suspend fun reopen(key: String) {
        dataStore.edit { it[Keys.CLOSED_KEYS] = it[Keys.CLOSED_KEYS].orEmpty() - key }
    }

    suspend fun snooze(key: String, until: LocalDate) {
        dataStore.edit { prefs ->
            val snoozed = prefs[Keys.SNOOZED_UNTIL].orEmpty().decodeEntries(::parseDate)
            prefs[Keys.SNOOZED_UNTIL] = (snoozed + (key to until)).encodeEntries()
            prefs.removeFromSet(Keys.OPENED_KEYS, key)
        }
    }

    private fun MutablePreferences.removeFromSet(setKey: Preferences.Key<Set<String>>, value: String) {
        this[setKey] = this[setKey].orEmpty() - value
    }

    private fun <T : Any> perGame(read: (GameId) -> T?): Map<GameId, T> =
        GameId.entries.mapNotNull { gameId -> read(gameId)?.let { gameId to it } }.toMap()
}

private const val ENTRY_SEPARATOR = '='

private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

private fun <T : Any> Set<String>.decodeEntries(parse: (String) -> T?): Map<String, T> =
    mapNotNull { entry ->
        val key = entry.substringBeforeLast(ENTRY_SEPARATOR, missingDelimiterValue = "")
        val value = parse(entry.substringAfterLast(ENTRY_SEPARATOR))
        if (key.isEmpty() || value == null) null else key to value
    }.toMap()

private fun Map<String, *>.encodeEntries(): Set<String> = map { (key, value) -> "$key$ENTRY_SEPARATOR$value" }.toSet()
