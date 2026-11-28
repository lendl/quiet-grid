package com.quietgrid.app.data

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeFacts
import com.quietgrid.app.core.badges.FinishFacts
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.datetime.LocalDate

class BadgeRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val today = LocalDate(2026, 11, 14)

    private fun finish(gameId: GameId, difficulty: Difficulty, solved: Boolean, facts: BadgeFacts = BadgeFacts()) = FinishFacts(
        gameId = gameId,
        difficulty = difficulty,
        solved = solved,
        elapsedSeconds = 60,
        date = today,
        startedDate = null,
        journeyStopId = null,
        mixId = null,
        facts = facts,
    )

    private fun repository(
        scope: CoroutineScope,
        journeyStore: FakeJourneyStore = FakeJourneyStore(),
        journeyFiles: Map<GameId, JourneyFile> = emptyMap(),
        mix: Mix? = null,
    ) = BadgeRepository(
        dataStore = preferencesDataStoreForTest(scope) { tempFolder.newFile("badges_test.preferences_pb") },
        historyStore = FakeHistoryStore(),
        journeyStore = journeyStore,
        journeyFile = { journeyFiles[it] },
        activeMix = { mix },
        today = { today },
    )

    @Test
    fun `two qualifying badges are announced one finish at a time`() = runTest {
        val badges = repository(backgroundScope)

        badges.recordFinish(finish(GameId.WORDGUESS, Difficulty.EASY, solved = true, facts = BadgeFacts(guesses = 2)))
        assertEquals("wordguess_in_three", badges.takeAnnouncement())
        assertEquals(setOf("wordguess_in_three"), badges.earned.first().keys)

        badges.recordFinish(finish(GameId.WORDGUESS, Difficulty.EASY, solved = false, facts = BadgeFacts(guesses = 6)))
        assertEquals("wordguess_in_two", badges.announcement.first())
        assertEquals(setOf("wordguess_in_three", "wordguess_in_two"), badges.earned.first().keys)

        badges.recordFinish(finish(GameId.WORDGUESS, Difficulty.EASY, solved = false, facts = BadgeFacts(guesses = 6)))
        assertNull(badges.announcement.first())
    }

    @Test
    fun `backfill earns journey badges silently`() = runTest {
        val journeyStore = FakeJourneyStore().apply {
            progress.value = mapOf("t1" to JourneyStopProgress(stars = 2, cleared = true))
        }
        val file = JourneyFile(
            game = "takuzu",
            chapters = listOf(JourneyChapter(1, 0, 100, listOf(JourneyStop("t1", "easy", 10, 60)))),
        )
        val badges = repository(backgroundScope, journeyStore, mapOf(GameId.TAKUZU to file))

        val earned = badges.earned.first()

        assertEquals(setOf("takuzu_chapter_one", "takuzu_journey_end"), earned.keys)
        assertNull(badges.announcement.first())
        assertEquals("takuzu_journey_end", badges.overview().first().seenNewest)
    }

    @Test
    fun `beta finishes leave the queue and progress alone`() = runTest {
        val badges = repository(backgroundScope)
        badges.recordFinish(finish(GameId.GAME_2048, Difficulty.EASY, solved = false, facts = BadgeFacts(maxTile = 2048)))
        assertEquals("game2048_1024", badges.takeAnnouncement())

        badges.recordFinish(finish(GameId.HASHI, Difficulty.EXPERT, solved = true))

        assertNull(badges.announcement.first())
        assertEquals(setOf("game2048_1024"), badges.earned.first().keys)
        assertTrue("hashi" !in badges.overview().first().progress.solvedGames)
    }

    @Test
    fun `solves in the active mix count toward mixed bag`() = runTest {
        val mix = Mix(
            id = "mix-a",
            name = "Mix",
            entries = listOf(
                MixEntry("sudoku", MixEntryMode.PUZZLE, "easy", 5),
                MixEntry("takuzu", MixEntryMode.PUZZLE, "easy", 5),
                MixEntry("nonogram", MixEntryMode.CHALLENGER, null, 5),
            ),
        )
        val badges = repository(backgroundScope, mix = mix)

        badges.recordFinish(finish(GameId.SUDOKU, Difficulty.EASY, solved = true))
        badges.recordFinish(finish(GameId.TAKUZU, Difficulty.EASY, solved = true))

        assertEquals("mixed_bag", badges.announcement.first())
        assertEquals(setOf("sudoku", "takuzu"), badges.overview().first().progress.mixSolvedGames["mix-a"])
    }
}
