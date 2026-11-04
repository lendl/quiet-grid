package com.quietgrid.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JourneyProgressRepositoryTest {
    private lateinit var database: QuietGridDatabase
    private val history = FakeHistoryStore()
    private val game = GameId.ANIMALDOKU

    private val file = JourneyFile(
        game = game.key,
        chapters = listOf(
            JourneyChapter(
                index = 1,
                minRating = 100,
                maxRating = 400,
                stops = listOf(
                    JourneyStop("s1", "easy", 150, 90),
                    JourneyStop("s2", "easy", 200, 90),
                    JourneyStop("s3", "medium", 300, 120, replaces = "old3"),
                ),
            ),
        ),
    )

    private val pools = mapOf(
        Difficulty.EASY to listOf("s1" to 150, "s2" to 200, "sib-near" to 155, "sib-far" to 190),
        Difficulty.MEDIUM to listOf("s3" to 300, "m-sib" to 310),
    )

    private lateinit var repository: JourneyProgressRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuietGridDatabase::class.java).allowMainThreadQueries().build()
        repository = JourneyProgressRepository(
            dao = database.journeyProgressDao(),
            historyStore = history,
            journeyFile = { if (it == game) file else null },
            ratedPool = { gameId, difficulty -> if (gameId == game) pools[difficulty].orEmpty() else emptyList() },
            clock = { 5000L },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `win then loss keep stars and point the stop at a sibling`() = runBlocking {
        val runStars = repository.recordWin(game, "s1", JourneySignals(hintsUsed = 0, mistakes = 0), elapsedSeconds = 60, score = 750)
        repository.recordLoss(game, "s1")

        val progress = repository.progressFor(game).first().getValue("s1")
        assertEquals(3, runStars)
        assertEquals(3, progress.stars)
        assertTrue(progress.cleared)
        assertEquals(60, progress.bestSeconds)
        assertEquals(750, progress.bestScore)
        assertEquals(2, progress.attempts)
        assertEquals("sib-near", progress.nextPuzzleId)
    }

    @Test
    fun `loss avoids played siblings and uses the stop's own tier`() = runBlocking {
        history.appendRecord(PlayRecord(game.key, "easy", "sib-near", solved = false, score = 0, elapsedSeconds = 10, timestampMillis = 1))
        repository.recordLoss(game, "s1")
        repository.recordLoss(game, "s3")

        val progress = repository.progressFor(game).first()
        assertEquals("sib-far", progress.getValue("s1").nextPuzzleId)
        assertEquals("m-sib", progress.getValue("s3").nextPuzzleId)
    }

    @Test
    fun `win uses the stop's target time`() = runBlocking {
        repository.recordWin(game, "s3", JourneySignals(hintsUsed = 0, mistakes = 0), elapsedSeconds = 100, score = 0)
        repository.recordWin(game, "s2", JourneySignals(hintsUsed = 0, mistakes = 0), elapsedSeconds = 100, score = 0)

        val progress = repository.progressFor(game).first()
        assertTrue(progress.getValue("s3").underTarget)
        assertFalse(progress.getValue("s2").underTarget)
    }

    @Test
    fun `skip marks the stop skipped without clearing it`() = runBlocking {
        repository.skip(game, "s2")

        val progress = repository.progressFor(game).first().getValue("s2")
        assertTrue(progress.skipped)
        assertFalse(progress.cleared)
        assertEquals(0, progress.stars)
    }

    @Test
    fun `progress under a replaced id moves to the new id`() = runBlocking {
        database.journeyProgressDao().upsert(
            JourneyProgressEntity(game.key, "old3", 2, true, true, false, 80, null, false, 1, null, 1000),
        )

        val progress = repository.progressFor(game).first()
        assertEquals(2, progress.getValue("s3").stars)
        assertEquals(80, progress.getValue("s3").bestSeconds)
        assertFalse("old3" in progress)
        assertNull(database.journeyProgressDao().get(game.key, "old3"))
    }

    @Test
    fun `replaced id does not overwrite progress already on the new id`() = runBlocking {
        val dao = database.journeyProgressDao()
        dao.upsert(JourneyProgressEntity(game.key, "old3", 1, true, false, false, 200, null, false, 1, null, 1000))
        dao.upsert(JourneyProgressEntity(game.key, "s3", 3, true, true, true, 50, null, false, 1, null, 2000))

        assertEquals(3, repository.progressFor(game).first().getValue("s3").stars)
    }
}
