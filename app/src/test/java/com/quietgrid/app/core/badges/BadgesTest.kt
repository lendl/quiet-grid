package com.quietgrid.app.core.badges

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 11, 14)
private val NO_JOURNEYS = JourneySnapshot(emptyMap(), emptyMap(), emptyMap())

private fun finish(
    gameId: GameId,
    difficulty: Difficulty = Difficulty.EASY,
    solved: Boolean = true,
    facts: BadgeFacts = BadgeFacts(),
    elapsedSeconds: Int = 60,
    startedDate: LocalDate? = null,
    mixId: String? = null,
) = FinishFacts(
    gameId = gameId,
    difficulty = difficulty,
    solved = solved,
    elapsedSeconds = elapsedSeconds,
    date = TODAY,
    startedDate = startedDate,
    journeyStopId = null,
    mixId = mixId,
    facts = facts,
)

private fun qualifying(
    finish: FinishFacts,
    progress: BadgeProgress = BadgeProgress(),
    journeys: JourneySnapshot = NO_JOURNEYS,
    mixGames: Set<String>? = null,
    nonBeta: Set<String> = NON_BETA_GAME_KEYS,
) = qualifyingBadges(finish, progress, journeys, mixGames, nonBeta)

private fun journeyFile(gameId: GameId, vararg chapters: List<String>) = JourneyFile(
    game = gameId.key,
    chapters = chapters.mapIndexed { index, stops ->
        JourneyChapter(index + 1, 0, 100, stops.map { JourneyStop(puzzleId = it, difficulty = "easy", rating = 10, targetSeconds = 60) })
    },
)

class BadgesTest {

    @Test
    fun `the catalog holds 83 badges with unique ids and no beta games`() {
        assertEquals(83, BadgeCatalog.all.size)
        assertEquals(83, BadgeCatalog.all.map { it.id }.toSet().size)
        assertEquals(8, BadgeCatalog.general.size)
        assertTrue(BadgeCatalog.all.mapNotNull { it.gameId }.none { GameCatalog.get(it).beta })
        assertEquals(4, BadgeCatalog.forGame(GameId.SUDOKU).size)
        assertEquals(3, BadgeCatalog.forGame(GameId.MINESWEEPER).size)
        assertTrue(BadgeCatalog.forGame(GameId.HASHI).isEmpty())
        assertTrue(BadgeCatalog.byId("game2048_4096") != null)
    }

    @Test
    fun `a first hard sudoku solve with no mistakes earns nothing`() {
        val result = qualifying(finish(GameId.SUDOKU, Difficulty.HARD, facts = BadgeFacts(hintsUsed = 0, mistakes = 0)))

        assertEquals(emptyList<String>(), result)
        val ledger = BadgeLedger().afterFinish(result, TODAY)
        assertTrue(ledger.earned.isEmpty())
        assertNull(ledger.announcement)
    }

    @Test
    fun `an expert sudoku solve without hints qualifies the sudoku skill badge`() {
        val result = qualifying(finish(GameId.SUDOKU, Difficulty.EXPERT, facts = BadgeFacts(hintsUsed = 0)))

        assertEquals(listOf("sudoku_summit"), result)
        assertEquals(emptyList<String>(), qualifying(finish(GameId.SUDOKU, Difficulty.EXPERT, facts = BadgeFacts(hintsUsed = 1))))
    }

    @Test
    fun `a finish that qualifies for two badges earns one and the next finish earns the queued one`() {
        val first = qualifying(finish(GameId.THEMECLEAR, Difficulty.EXPERT, facts = BadgeFacts(hintsUsed = 0)))
        assertEquals(listOf("themeclear_spotless", "themeclear_deep_theme", "themeclear_deep_and_spotless"), first)

        val afterFirst = BadgeLedger().afterFinish(first, TODAY)
        assertEquals(setOf("themeclear_spotless"), afterFirst.earned.keys)
        assertEquals(listOf("themeclear_deep_theme", "themeclear_deep_and_spotless"), afterFirst.queue)
        assertEquals("themeclear_spotless", afterFirst.announcement)

        val loss = qualifying(finish(GameId.THEMECLEAR, Difficulty.EASY, solved = false))
        val afterLoss = afterFirst.afterFinish(loss, TODAY.plusDays(1))
        assertEquals("themeclear_deep_theme", afterLoss.announcement)
        assertEquals(TODAY.plusDays(1), afterLoss.earned["themeclear_deep_theme"])
        assertEquals(listOf("themeclear_deep_and_spotless"), afterLoss.queue)
    }

    @Test
    fun `a two guess word guess solve also qualifies in three but announces one`() {
        val result = qualifying(finish(GameId.WORDGUESS, facts = BadgeFacts(guesses = 2)))

        assertEquals(listOf("wordguess_in_three", "wordguess_in_two"), result)
        val ledger = BadgeLedger().afterFinish(result, TODAY)
        assertEquals("wordguess_in_three", ledger.announcement)
        assertEquals(setOf("wordguess_in_three"), ledger.earned.keys)
        assertEquals(listOf("wordguess_in_two"), ledger.queue)
    }

    @Test
    fun `a 2048 run reaching 4096 queues the bigger tiles behind 1024`() {
        val result = qualifying(finish(GameId.GAME_2048, solved = false, facts = BadgeFacts(maxTile = 4096)))

        val ledger = BadgeLedger().afterFinish(result, TODAY)
        assertEquals("game2048_1024", ledger.announcement)
        assertEquals(listOf("game2048_2048", "game2048_4096"), ledger.queue)
    }

    @Test
    fun `already earned or queued badges are not added again`() {
        val ledger = BadgeLedger(earned = mapOf("game2048_1024" to TODAY), queue = listOf("game2048_2048"))

        val next = ledger.afterFinish(listOf("game2048_1024", "game2048_2048", "game2048_4096"), TODAY)

        assertEquals("game2048_2048", next.announcement)
        assertEquals(listOf("game2048_4096"), next.queue)
    }

    @Test
    fun `beta games never qualify and never add to solved games`() {
        val hashi = finish(GameId.HASHI, Difficulty.EXPERT)

        assertEquals(emptyList<String>(), qualifying(hashi))
        assertEquals(BadgeProgress(), updatedProgress(BadgeProgress(), hashi))
    }

    @Test
    fun `full tour uses the non beta set passed in`() {
        val progress = BadgeProgress(solvedGames = setOf("takuzu"))
        val sudoku = finish(GameId.SUDOKU)

        assertTrue("full_tour" in qualifying(sudoku, progress, nonBeta = setOf("sudoku", "takuzu")))
        assertFalse("full_tour" in qualifying(sudoku, progress))
    }

    @Test
    fun `mixed bag only counts solves made while that mix was active`() {
        val mixGames = setOf("sudoku", "takuzu")
        val inMix = updatedProgress(BadgeProgress(), finish(GameId.SUDOKU, mixId = "mix-a"))
        val outsideMix = updatedProgress(inMix, finish(GameId.TAKUZU))

        assertFalse("mixed_bag" in qualifying(finish(GameId.NONOGRAM, mixId = "mix-a"), outsideMix, mixGames = mixGames))
        assertFalse("mixed_bag" in qualifying(finish(GameId.TAKUZU, mixId = "mix-b"), outsideMix, mixGames = mixGames))
        assertTrue("mixed_bag" in qualifying(finish(GameId.TAKUZU, mixId = "mix-a"), outsideMix, mixGames = mixGames))
    }

    @Test
    fun `picked back up needs a start date before the finish date`() {
        assertFalse("picked_back_up" in qualifying(finish(GameId.SUDOKU, startedDate = null)))
        assertFalse("picked_back_up" in qualifying(finish(GameId.SUDOKU, startedDate = TODAY)))
        assertTrue("picked_back_up" in qualifying(finish(GameId.SUDOKU, startedDate = TODAY.minusDays(1))))
        assertFalse("picked_back_up" in qualifying(finish(GameId.SUDOKU, solved = false, startedDate = TODAY.minusDays(1))))
    }

    @Test
    fun `slow and steady needs a solve of at least thirty minutes`() {
        assertFalse("slow_and_steady" in qualifying(finish(GameId.SUDOKU, elapsedSeconds = 1799)))
        assertTrue("slow_and_steady" in qualifying(finish(GameId.SUDOKU, elapsedSeconds = 1800)))
    }

    @Test
    fun `pumpkin patch needs a halloween puzzle`() {
        assertFalse("pumpkin_patch" in qualifying(finish(GameId.WORDSEARCH)))
        assertTrue("pumpkin_patch" in qualifying(finish(GameId.WORDSEARCH, facts = BadgeFacts(seasonal = SEASON_HALLOWEEN))))
    }

    @Test
    fun `journey badges come after the game's own badges`() {
        val journeys = JourneySnapshot(
            chapterFinished = mapOf(GameId.SUDOKU to true),
            journeyFinished = mapOf(GameId.SUDOKU to false),
            allStars = mapOf(GameId.SUDOKU to false),
        )

        val result = qualifying(finish(GameId.SUDOKU, Difficulty.EXPERT), journeys = journeys)

        assertEquals(listOf("sudoku_summit", "sudoku_chapter_one"), result)
    }

    @Test
    fun `journey snapshot reads chapters and stars from stop progress`() {
        val file = journeyFile(GameId.SUDOKU, listOf("a", "b"), listOf("c"))
        val cleared = JourneyStopProgress(stars = 3, cleared = true)
        val progress = mapOf("a" to cleared, "b" to cleared, "c" to JourneyStopProgress(stars = 0, skipped = true))

        val snapshot = journeySnapshot(mapOf(GameId.SUDOKU to file), mapOf(GameId.SUDOKU to progress))

        assertEquals(true, snapshot.chapterFinished[GameId.SUDOKU])
        assertEquals(false, snapshot.journeyFinished[GameId.SUDOKU])
        assertEquals(false, snapshot.allStars[GameId.SUDOKU])
    }

    @Test
    fun `journey snapshot reads progress saved under a replaced stop id`() {
        val file = JourneyFile(
            game = "sudoku",
            chapters = listOf(JourneyChapter(1, 0, 100, listOf(JourneyStop("new-id", "easy", 10, 60, replaces = "old-id")))),
        )

        val snapshot = journeySnapshot(mapOf(GameId.SUDOKU to file), mapOf(GameId.SUDOKU to mapOf("old-id" to JourneyStopProgress(stars = 3, cleared = true))))

        assertEquals(true, snapshot.journeyFinished[GameId.SUDOKU])
        assertEquals(true, snapshot.allStars[GameId.SUDOKU])
    }

    @Test
    fun `backfill earns journey badges and proven general badges from existing data`() {
        val journeys = journeySnapshot(
            mapOf(GameId.TAKUZU to journeyFile(GameId.TAKUZU, listOf("t1"))),
            mapOf(GameId.TAKUZU to mapOf("t1" to JourneyStopProgress(stars = 3, cleared = true))),
        )
        val records = listOf(
            PlayRecord(gameId = "sudoku", difficulty = "expert", solved = true, score = 1, elapsedSeconds = 2000, timestampMillis = 1L),
            PlayRecord(gameId = "nonogram", difficulty = "expert", solved = true, score = 1, elapsedSeconds = 10, timestampMillis = 2L, isChallenger = true),
            PlayRecord(gameId = "takuzu", difficulty = "hard", solved = false, score = 0, elapsedSeconds = 10, timestampMillis = 3L),
        )

        val backfill = backfillBadges(records, journeys)

        assertEquals(
            setOf("takuzu_chapter_one", "takuzu_journey_end", "takuzu_every_star", "slow_and_steady"),
            backfill.earned,
        )
        assertEquals(setOf("sudoku"), backfill.progress.solvedGames)
        assertEquals(setOf("sudoku"), backfill.progress.expertGames)
    }

    @Test
    fun `badge counts follow natural totals`() {
        val progress = BadgeProgress(solvedGames = setOf("sudoku", "takuzu", "hashi"), expertGames = setOf("sudoku"))

        assertEquals(BadgeCount(2, NON_BETA_GAME_KEYS.size), badgeCount("full_tour", progress, NO_JOURNEYS))
        assertEquals(BadgeCount(1, EXPERT_OF_MANY_GAMES), badgeCount("expert_of_many", progress, NO_JOURNEYS))
        assertEquals(BadgeCount(0, BADGE_JOURNEY_GAMES.size), badgeCount("all_journeys", progress, NO_JOURNEYS))
        assertNull(badgeCount("pumpkin_patch", progress, NO_JOURNEYS))
    }
}
