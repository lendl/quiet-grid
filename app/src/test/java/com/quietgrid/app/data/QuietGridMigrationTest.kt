package com.quietgrid.app.data

import android.app.Application
import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QuietGridMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(QUIET_GRID_DATABASE_FILE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(QUIET_GRID_DATABASE_FILE)
    }

    private fun createVersion2Database() = createLegacyDatabase(2) {}

    private fun createVersion3Database() = createLegacyDatabase(3) { connection -> MIGRATION_2_3.migrate(connection) }

    private fun createVersion4Database() = createLegacyDatabase(4) { connection ->
        MIGRATION_2_3.migrate(connection)
        MIGRATION_3_4.migrate(connection)
        connection.execSQL("INSERT INTO journey_progress (scope, puzzleId, stars, cleared, clean, underTarget, bestSeconds, skipped, attempts, nextPuzzleId, lastPlayedMillis) VALUES ('animaldoku', 'ad-1', 2, 1, 1, 0, 95, 0, 1, NULL, 3000)")
    }

    private fun createVersion5Database() = createLegacyDatabase(5) { connection ->
        MIGRATION_2_3.migrate(connection)
        MIGRATION_3_4.migrate(connection)
        MIGRATION_4_5.migrate(connection)
        connection.execSQL("INSERT INTO journey_progress (scope, puzzleId, stars, cleared, clean, underTarget, bestSeconds, bestScore, skipped, attempts, nextPuzzleId, lastPlayedMillis) VALUES ('animaldoku', 'ad-1', 3, 1, 1, 1, 80, 700, 0, 2, 'ad-9', 3000)")
    }

    private fun createVersion6Database() = createLegacyDatabase(6) { connection ->
        MIGRATION_2_3.migrate(connection)
        MIGRATION_3_4.migrate(connection)
        MIGRATION_4_5.migrate(connection)
        MIGRATION_5_6.migrate(connection)
        connection.execSQL("INSERT INTO journey_progress (scope, puzzleId, stars, cleared, clean, underTarget, bestSeconds, bestScore, skipped, attempts, nextPuzzleId, lastPlayedMillis) VALUES ('animaldoku', 'ad-1', 2, 1, 1, 0, 95, 640, 0, 1, 'ad-9', 3000)")
        connection.execSQL("INSERT INTO journey_milestone (scope, kind, chapterIndex, achievedMillis, shownMillis) VALUES ('animaldoku', 'chapter', 1, 4000, NULL)")
    }

    private fun openDatabase() = quietGridDatabase(context)

    private fun createLegacyDatabase(version: Int, upgrade: (SQLiteConnection) -> Unit) {
        val connection = AndroidSQLiteDriver().open(context.getDatabasePath(QUIET_GRID_DATABASE_FILE).absolutePath)
        try {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `play_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gameId` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `puzzleId` TEXT, `solved` INTEGER NOT NULL, `score` INTEGER NOT NULL, `elapsedSeconds` INTEGER NOT NULL, `timestampMillis` INTEGER NOT NULL, `lossReason` TEXT, `isChallenger` INTEGER NOT NULL, `puzzlesSolved` INTEGER)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId` ON `play_records` (`gameId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_difficulty` ON `play_records` (`gameId`, `difficulty`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_puzzleId_difficulty` ON `play_records` (`gameId`, `puzzleId`, `difficulty`)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a472c60e6443638bd6d2e0f599e93725')")
            connection.execSQL("INSERT INTO play_records (gameId, difficulty, puzzleId, solved, score, elapsedSeconds, timestampMillis, lossReason, isChallenger, puzzlesSolved) VALUES ('sudoku', 'hard', 's-1', 1, 120, 300, 1000, NULL, 0, NULL)")
            upgrade(connection)
            connection.execSQL("PRAGMA user_version = $version")
        } finally {
            connection.close()
        }
    }

    @Test
    fun `migration keeps existing records and adds nullable daily columns`() = runBlocking {
        createVersion2Database()

        val database = openDatabase()

        val records = database.playHistoryDao().allRecords().first()
        assertEquals(1, records.size)
        assertEquals("s-1", records.single().puzzleId)
        assertNull(records.single().dailyDate)
        assertNull(records.single().shareDetail)

        database.playHistoryDao().insert(
            PlayRecordEntity(
                gameId = "sudoku",
                difficulty = "easy",
                puzzleId = "s-2",
                solved = true,
                score = 50,
                elapsedSeconds = 60,
                timestampMillis = 2000,
                lossReason = null,
                dailyDate = "2026-09-24",
                shareDetail = "grid",
            ),
        )
        val after = database.playHistoryDao().allRecords().first()
        assertEquals("2026-09-24", after.first { it.puzzleId == "s-2" }.dailyDate)
        database.close()
    }

    @Test
    fun `migration 3 to 4 keeps records and adds journey progress`() = runBlocking {
        createVersion3Database()

        val database = openDatabase()

        val records = database.playHistoryDao().allRecords().first()
        assertEquals(listOf("s-1"), records.map { it.puzzleId })
        assertNull(records.single().journeyStop)

        val progress = JourneyProgressEntity(
            scope = "animaldoku",
            puzzleId = "ad-1",
            stars = 2,
            cleared = true,
            clean = true,
            underTarget = false,
            bestSeconds = 95,
            bestScore = 640,
            skipped = false,
            attempts = 1,
            nextPuzzleId = null,
            lastPlayedMillis = 3000,
        )
        database.journeyProgressDao().upsert(progress)
        assertEquals(progress, database.journeyProgressDao().get("animaldoku", "ad-1"))
        assertEquals(listOf(progress), database.journeyProgressDao().progressFor("animaldoku").first())
        database.close()
    }

    @Test
    fun `migration 4 to 5 keeps journey progress and adds score and stars columns`() = runBlocking {
        createVersion4Database()

        val database = openDatabase()

        val record = database.playHistoryDao().allRecords().first().single()
        assertEquals("s-1", record.puzzleId)
        assertNull(record.journeyStars)

        val progress = database.journeyProgressDao().get("animaldoku", "ad-1")!!
        assertEquals(2, progress.stars)
        assertEquals(95, progress.bestSeconds)
        assertNull(progress.bestScore)

        database.playHistoryDao().insert(
            PlayRecordEntity(
                gameId = "animaldoku",
                difficulty = "easy",
                puzzleId = "ad-2",
                solved = true,
                score = 500,
                elapsedSeconds = 40,
                timestampMillis = 4000,
                lossReason = null,
                journeyStop = "ad-2",
                journeyStars = 3,
            ),
        )
        assertEquals(3, database.playHistoryDao().allRecords().first().first { it.puzzleId == "ad-2" }.journeyStars)
        database.close()
    }

    @Test
    fun `migration 5 to 6 keeps journey progress and adds an empty milestone table`() = runBlocking {
        createVersion5Database()

        val database = openDatabase()
        val dao = database.journeyProgressDao()

        val progress = dao.get("animaldoku", "ad-1")!!
        assertEquals(3, progress.stars)
        assertEquals(700, progress.bestScore)
        assertEquals("ad-9", progress.nextPuzzleId)
        assertEquals("s-1", database.playHistoryDao().allRecords().first().single().puzzleId)
        assertEquals(emptyList<JourneyMilestoneEntity>(), dao.pendingMilestones("animaldoku"))

        val milestone = JourneyMilestoneEntity("animaldoku", JOURNEY_MILESTONE_CHAPTER, 1, achievedMillis = 5000, shownMillis = null)
        dao.upsertMilestone(milestone)
        dao.upsertMilestone(milestone.copy(achievedMillis = 6000))
        assertEquals(listOf(milestone.copy(achievedMillis = 6000)), dao.pendingMilestones("animaldoku"))
        database.close()
    }

    @Test
    fun `migration 6 to 7 keeps journey progress and milestones and starts with nothing retired`() = runBlocking {
        createVersion6Database()

        val database = openDatabase()
        val dao = database.journeyProgressDao()

        val progress = dao.get("animaldoku", "ad-1")!!
        assertEquals(2, progress.stars)
        assertEquals(640, progress.bestScore)
        assertEquals("ad-9", progress.nextPuzzleId)
        assertEquals("", progress.retiredPuzzleIds)
        assertEquals(listOf(1), dao.pendingMilestones("animaldoku").map { it.chapterIndex })

        dao.upsert(progress.copy(retiredPuzzleIds = "ad-1\nad-9"))
        assertEquals("ad-1\nad-9", dao.get("animaldoku", "ad-1")!!.retiredPuzzleIds)
        database.close()
    }
}
