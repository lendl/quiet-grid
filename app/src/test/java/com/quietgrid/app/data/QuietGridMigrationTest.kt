package com.quietgrid.app.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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

private const val DB_NAME = "migration-test.db"

@RunWith(RobolectricTestRunner::class)
class QuietGridMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    private fun createVersion2Database() = createLegacyDatabase(2) {}

    private fun createVersion3Database() = createLegacyDatabase(3) { db -> MIGRATION_2_3.migrate(db) }

    private fun createVersion4Database() = createLegacyDatabase(4) { db ->
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
        db.execSQL("INSERT INTO journey_progress (scope, puzzleId, stars, cleared, clean, underTarget, bestSeconds, skipped, attempts, nextPuzzleId, lastPlayedMillis) VALUES ('animaldoku', 'ad-1', 2, 1, 1, 0, 95, 0, 1, NULL, 3000)")
    }

    private fun openDatabase() = Room.databaseBuilder(context, QuietGridDatabase::class.java, DB_NAME)
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .allowMainThreadQueries()
        .build()

    private fun createLegacyDatabase(version: Int, upgrade: (SupportSQLiteDatabase) -> Unit) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `play_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gameId` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `puzzleId` TEXT, `solved` INTEGER NOT NULL, `score` INTEGER NOT NULL, `elapsedSeconds` INTEGER NOT NULL, `timestampMillis` INTEGER NOT NULL, `lossReason` TEXT, `isChallenger` INTEGER NOT NULL, `puzzlesSolved` INTEGER)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId` ON `play_records` (`gameId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_difficulty` ON `play_records` (`gameId`, `difficulty`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_puzzleId_difficulty` ON `play_records` (`gameId`, `puzzleId`, `difficulty`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                    db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a472c60e6443638bd6d2e0f599e93725')")
                    db.execSQL("INSERT INTO play_records (gameId, difficulty, puzzleId, solved, score, elapsedSeconds, timestampMillis, lossReason, isChallenger, puzzlesSolved) VALUES ('sudoku', 'hard', 's-1', 1, 120, 300, 1000, NULL, 0, NULL)")
                    upgrade(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()
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
}
