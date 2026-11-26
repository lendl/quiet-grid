package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BADGE_JOURNEY_GAMES
import com.quietgrid.app.core.badges.BadgeLedger
import com.quietgrid.app.core.badges.BadgeProgress
import com.quietgrid.app.core.badges.FinishFacts
import com.quietgrid.app.core.badges.JourneySnapshot
import com.quietgrid.app.core.badges.NON_BETA_GAME_KEYS
import com.quietgrid.app.core.badges.afterFinish
import com.quietgrid.app.core.badges.backfillBadges
import com.quietgrid.app.core.badges.journeySnapshot
import com.quietgrid.app.core.badges.mixBadgeGames
import com.quietgrid.app.core.badges.qualifyingBadges
import com.quietgrid.app.core.badges.updatedProgress
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.engine.journey.JourneyFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

interface BadgeStore {
    val earned: Flow<Map<String, LocalDate>>
    val announcement: Flow<String?>
    suspend fun recordFinish(finish: FinishFacts)
    suspend fun clearAnnouncement()
}

data class BadgeOverview(
    val earned: Map<String, LocalDate>,
    val progress: BadgeProgress,
    val journeys: JourneySnapshot,
    val seenNewest: String?,
)

private val badgeJson = Json { ignoreUnknownKeys = true }

private object BadgeKeys {
    val EARNED = stringPreferencesKey("badges_earned")
    val QUEUE = stringPreferencesKey("badges_queue")
    val PROGRESS = stringPreferencesKey("badges_progress")
    val ANNOUNCE = stringPreferencesKey("badges_announce")
    val BACKFILLED = booleanPreferencesKey("badges_backfilled")
    val SEEN_NEWEST = stringPreferencesKey("badges_seen_newest")
}

class BadgeRepository(
    private val dataStore: DataStore<Preferences>,
    private val historyStore: PlayHistoryStore,
    private val journeyStore: JourneyProgressStore,
    private val journeyFile: suspend (GameId) -> JourneyFile?,
    private val activeMix: suspend () -> Mix?,
    private val today: () -> LocalDate = LocalDate::now,
) : BadgeStore {

    constructor(
        context: Context,
        dataStore: DataStore<Preferences>,
        historyStore: PlayHistoryStore,
        journeyStore: JourneyProgressStore,
        mixRepository: MixRepository,
        settingsRepository: SettingsRepository,
    ) : this(
        dataStore = dataStore,
        historyStore = historyStore,
        journeyStore = journeyStore,
        journeyFile = { loadJourneyFile(context, settingsRepository.journeyTrack(it).first()) },
        activeMix = {
            val activeId = mixRepository.activeMixId.first()
            activeId?.let { id -> mixRepository.mixes.first().firstOrNull { it.id == id } }
        },
    )

    private val mutex = Mutex()

    @Volatile private var backfilled = false

    override val earned: Flow<Map<String, LocalDate>> = flow {
        ensureBackfilled()
        emitAll(dataStore.data.map { it.earned() }.distinctUntilChanged())
    }

    override val announcement: Flow<String?> = dataStore.data.map { it[BadgeKeys.ANNOUNCE] }.distinctUntilChanged()

    fun overview(): Flow<BadgeOverview> = flow {
        ensureBackfilled()
        val journeys = currentJourneys()
        emitAll(
            dataStore.data
                .map { prefs -> BadgeOverview(prefs.earned(), prefs.progress(), journeys, prefs[BadgeKeys.SEEN_NEWEST]) }
                .distinctUntilChanged(),
        )
    }

    override suspend fun recordFinish(finish: FinishFacts) {
        if (finish.gameId.key !in NON_BETA_GAME_KEYS) return
        ensureBackfilled()
        val mix = activeMix()
        val withMix = finish.copy(mixId = mix?.id)
        val journeys = currentJourneys()
        mutex.withLock {
            dataStore.edit { prefs ->
                val progress = prefs.progress()
                val qualifying = qualifyingBadges(withMix, progress, journeys, mix?.let { mixBadgeGames(it) }, NON_BETA_GAME_KEYS)
                prefs.writeLedger(prefs.ledger().afterFinish(qualifying, finish.date))
                prefs[BadgeKeys.PROGRESS] = badgeJson.encodeToString(BadgeProgress.serializer(), updatedProgress(progress, withMix))
            }
        }
    }

    override suspend fun clearAnnouncement() {
        dataStore.edit { it.remove(BadgeKeys.ANNOUNCE) }
    }

    suspend fun takeAnnouncement(): String? {
        var taken: String? = null
        dataStore.edit { prefs ->
            taken = prefs[BadgeKeys.ANNOUNCE]
            prefs.remove(BadgeKeys.ANNOUNCE)
        }
        return taken
    }

    suspend fun markSeen(id: String) {
        dataStore.edit { it[BadgeKeys.SEEN_NEWEST] = id }
    }

    private suspend fun ensureBackfilled() {
        if (backfilled) return
        mutex.withLock {
            if (backfilled) return
            if (dataStore.data.first()[BadgeKeys.BACKFILLED] != true) backfill()
            backfilled = true
        }
    }

    private suspend fun backfill() {
        val result = backfillBadges(historyStore.allRecords().first(), currentJourneys())
        val date = today()
        dataStore.edit { prefs ->
            val ledger = prefs.ledger()
            val earned = ledger.earned + result.earned.filterNot { it in ledger.earned }.associateWith { date }
            prefs.writeLedger(ledger.copy(earned = earned))
            val progress = prefs.progress()
            val merged = progress.copy(
                solvedGames = progress.solvedGames + result.progress.solvedGames,
                expertGames = progress.expertGames + result.progress.expertGames,
            )
            prefs[BadgeKeys.PROGRESS] = badgeJson.encodeToString(BadgeProgress.serializer(), merged)
            earned.keys.lastOrNull()?.let { prefs[BadgeKeys.SEEN_NEWEST] = it }
            prefs[BadgeKeys.BACKFILLED] = true
        }
    }

    private suspend fun currentJourneys(): JourneySnapshot {
        val files = BADGE_JOURNEY_GAMES.mapNotNull { gameId -> journeyFile(gameId)?.let { gameId to it } }.toMap()
        val progress = files.keys.associateWith { journeyStore.storedProgress(it) }
        return journeySnapshot(files, progress)
    }
}

private fun Preferences.earned(): Map<String, LocalDate> =
    this[BadgeKeys.EARNED]
        ?.let { raw -> runCatching { badgeJson.decodeFromString<Map<String, String>>(raw) }.getOrNull() }
        .orEmpty()
        .mapNotNull { (id, date) -> runCatching { LocalDate.parse(date) }.getOrNull()?.let { id to it } }
        .toMap()

private fun Preferences.queue(): List<String> =
    this[BadgeKeys.QUEUE]?.let { raw -> runCatching { badgeJson.decodeFromString<List<String>>(raw) }.getOrNull() }.orEmpty()

private fun Preferences.progress(): BadgeProgress =
    this[BadgeKeys.PROGRESS]?.let { raw -> runCatching { badgeJson.decodeFromString(BadgeProgress.serializer(), raw) }.getOrNull() }
        ?: BadgeProgress()

private fun Preferences.ledger(): BadgeLedger = BadgeLedger(earned(), queue(), this[BadgeKeys.ANNOUNCE])

private fun MutablePreferences.writeLedger(ledger: BadgeLedger) {
    this[BadgeKeys.EARNED] = badgeJson.encodeToString(ledger.earned.mapValues { it.value.toString() })
    this[BadgeKeys.QUEUE] = badgeJson.encodeToString(ledger.queue)
    val announcement = ledger.announcement
    if (announcement == null) remove(BadgeKeys.ANNOUNCE) else this[BadgeKeys.ANNOUNCE] = announcement
}
