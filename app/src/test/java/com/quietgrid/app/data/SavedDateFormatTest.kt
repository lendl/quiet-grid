package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeFacts
import com.quietgrid.app.core.badges.FinishFacts
import com.quietgrid.app.core.daily.dailyStreak
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private fun date(text: String): LocalDate = LocalDate.parse(text)

private fun time(hour: Int, minute: Int): LocalTime = LocalTime(hour, minute)

class SavedDateFormatTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newDataStore(scope: CoroutineScope): DataStore<Preferences> =
        preferencesDataStoreForTest(scope) { tempFolder.newFile() }

    private suspend fun DataStore<Preferences>.rawString(name: String): String? = data.first()[stringPreferencesKey(name)]

    private fun badgeRepository(dataStore: DataStore<Preferences>, today: LocalDate) = BadgeRepository(
        dataStore = dataStore,
        historyStore = FakeHistoryStore(),
        journeyStore = FakeJourneyStore(),
        journeyFile = { null },
        activeMix = { null },
        today = { today },
    )

    @Test
    fun `badges earned dates decode from the stored json map and encode back identically`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit {
            it[stringPreferencesKey("badges_earned")] = """{"takuzu_chapter_one":"2026-11-14"}"""
            it[androidx.datastore.preferences.core.booleanPreferencesKey("badges_backfilled")] = true
        }
        val repository = badgeRepository(dataStore, date("2026-11-20"))

        assertEquals(mapOf("takuzu_chapter_one" to "2026-11-14"), repository.earned.first().mapValues { it.value.toString() })

        repository.recordFinish(
            FinishFacts(
                gameId = GameId.SUDOKU,
                difficulty = Difficulty.EASY,
                solved = false,
                elapsedSeconds = 60,
                date = date("2026-11-20"),
                startedDate = null,
                journeyStopId = null,
                mixId = null,
                facts = BadgeFacts(),
            ),
        )

        assertEquals("""{"takuzu_chapter_one":"2026-11-14"}""", dataStore.rawString("badges_earned"))
    }

    @Test
    fun `badges backfill stamps the earned date as an iso date string`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        val repository = BadgeRepository(
            dataStore = dataStore,
            historyStore = FakeHistoryStore(),
            journeyStore = FakeJourneyStore().apply {
                progress.value = mapOf("t1" to com.quietgrid.app.core.journey.JourneyStopProgress(stars = 2, cleared = true))
            },
            journeyFile = { gameId ->
                if (gameId != GameId.TAKUZU) null else com.quietgrid.engine.journey.JourneyFile(
                    game = "takuzu",
                    chapters = listOf(
                        com.quietgrid.engine.journey.JourneyChapter(
                            1,
                            0,
                            100,
                            listOf(com.quietgrid.engine.journey.JourneyStop("t1", "easy", 10, 60)),
                        ),
                    ),
                )
            },
            activeMix = { null },
            today = { date("2026-11-14") },
        )

        repository.earned.first()

        assertEquals(
            """{"takuzu_chapter_one":"2026-11-14","takuzu_journey_end":"2026-11-14"}""",
            dataStore.rawString("badges_earned"),
        )
    }

    @Test
    fun `quiet tip last shown day and snoozed entries decode and encode back identically`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit {
            it[stringPreferencesKey("tip_last_shown_day")] = "2026-11-14"
            it[stringSetPreferencesKey("tip_snoozed_until")] = setOf("rotation_sudoku=2026-11-20")
        }
        val repository = QuietTipsRepository(dataStore)

        val state = repository.state().first()
        assertEquals("2026-11-14", state.lastShownDay.toString())
        assertEquals(mapOf("rotation_sudoku" to "2026-11-20"), state.snoozedUntil.mapValues { it.value.toString() })

        repository.markShown("rotation_sudoku", date("2026-11-15"))
        repository.snooze("rotation_sudoku", date("2026-11-21"))

        val prefs = dataStore.data.first()
        assertEquals("2026-11-15", prefs[stringPreferencesKey("tip_last_shown_day")])
        assertEquals(setOf("rotation_sudoku=2026-11-21"), prefs[stringSetPreferencesKey("tip_snoozed_until")])
    }

    @Test
    fun `daily reminder time is stored as minute of day`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[intPreferencesKey("daily_reminder_minute_of_day")] = 1230 }
        val repository = SettingsRepository(dataStore)

        val stored = repository.settings.first().dailyReminderTime
        assertEquals(20, stored.hour)
        assertEquals(30, stored.minute)

        repository.setDailyReminderTime(time(7, 5))

        assertEquals(425, dataStore.data.first()[intPreferencesKey("daily_reminder_minute_of_day")])
        val reread = repository.settings.first().dailyReminderTime
        assertEquals(7, reread.hour)
        assertEquals(5, reread.minute)
    }

    @Test
    fun `daily reminder default is nine sharp`() = runTest {
        val repository = SettingsRepository(newDataStore(backgroundScope))

        val default = repository.settings.first().dailyReminderTime

        assertEquals(9, default.hour)
        assertEquals(0, default.minute)
    }

    @Test
    fun `active session envelope keeps its date fields as iso strings`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        val literal =
            """{"gameId":"takuzu","elapsedSeconds":12.0,"payload":"p","dailyDate":"2026-09-24","dailyTier":"easy","difficulty":"easy","startedDate":"2026-09-20"}"""
        dataStore.edit { it[stringPreferencesKey("active_session")] = literal }
        val repository = SessionRepository(dataStore)

        val envelope = repository.activeSession.first()

        assertEquals("2026-09-24", envelope?.dailyDate)
        assertEquals("2026-09-20", envelope?.startedDate)

        repository.save(envelope!!)

        assertEquals(literal, dataStore.rawString("active_session"))
    }

    @Test
    fun `daily streak reads the iso daily date strings on play records`() {
        fun record(day: String) = PlayRecord(
            gameId = "takuzu",
            difficulty = "easy",
            solved = true,
            score = 0,
            elapsedSeconds = 30,
            timestampMillis = 0L,
            dailyDate = day,
        )

        val streak = dailyStreak(
            records = listOf(record("2026-11-12"), record("2026-11-13"), record("2026-11-14")),
            gameIds = setOf(GameId.TAKUZU),
            today = date("2026-11-14"),
        )

        assertEquals(3, streak)
    }
}
