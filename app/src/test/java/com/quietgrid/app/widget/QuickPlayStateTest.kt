package com.quietgrid.app.widget

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.Difficulty.EASY
import com.quietgrid.app.core.Difficulty.HARD
import com.quietgrid.app.core.Difficulty.MEDIUM
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.GameId.MINESWEEPER
import com.quietgrid.app.core.GameId.NBACK
import com.quietgrid.app.core.GameId.NONOGRAM
import com.quietgrid.app.core.GameId.STARBATTLE
import com.quietgrid.app.core.GameId.SUDOKU
import com.quietgrid.app.core.GameId.TAKUZU
import com.quietgrid.app.core.GameId.WORDSEARCH
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickPlayStateTest {

    private fun record(game: String, difficulty: String, ts: Long, isChallenger: Boolean = false) = PlayRecord(
        gameId = game,
        difficulty = difficulty,
        solved = true,
        score = 0,
        elapsedSeconds = 0,
        timestampMillis = ts,
        isChallenger = isChallenger,
    )

    private fun mix(id: String, vararg games: String) = Mix(id, "Mix $id", games.map { MixEntry(it, MixEntryMode.PUZZLE, "easy", 1) })

    private fun playMix(id: String, vararg games: GameId) = QuickPlayMix(id, "Mix $id", games.toList())

    private fun games(vararg pairs: Pair<GameId, Difficulty>) = pairs.map { QuickPlayGame(it.first, it.second) }

    private fun rankedGames(
        history: List<PlayRecord>,
        betaGamesEnabled: Boolean = false,
        favoriteGameIds: Set<String> = emptySet(),
    ) = quickPlayState(null, history, betaGamesEnabled, favoriteGameIds).games

    private fun widgetMix(mixes: List<Mix>, lastStartedMixId: String?, betaGamesEnabled: Boolean = false) =
        mixWidgetState(null, mixes, lastStartedMixId, betaGamesEnabled).mix

    private val defaults = games(SUDOKU to EASY, WORDSEARCH to EASY, MINESWEEPER to EASY)

    @Test
    fun `active session adds Continue with its game and difficulty while games stay`() {
        val state = quickPlayState(ActiveSessionSummary("sudoku", null, difficulty = "hard"), listOf(record("takuzu", "easy", 1)), false)
        assertEquals(QuickPlayContinue(SUDOKU, HARD), state.continueGame)
        assertEquals(games(TAKUZU to EASY, SUDOKU to EASY, WORDSEARCH to EASY), state.games)
    }

    @Test
    fun `active session without a known difficulty continues without one`() {
        assertEquals(QuickPlayContinue(SUDOKU, null), quickPlayContinue(ActiveSessionSummary("sudoku", null)))
        assertEquals(QuickPlayContinue(SUDOKU, null), quickPlayContinue(ActiveSessionSummary("sudoku", null, difficulty = "endless")))
    }

    @Test
    fun `no session or a session for an unknown game has no Continue`() {
        assertNull(quickPlayState(null, emptyList(), false).continueGame)
        assertNull(quickPlayState(ActiveSessionSummary("flowfree", null), emptyList(), false).continueGame)
    }

    @Test
    fun `mix widget shows Continue next to its mix`() {
        val state = mixWidgetState(ActiveSessionSummary("sudoku", null, difficulty = "hard"), listOf(mix("a", "takuzu")), "a", false)
        assertEquals(MixWidgetState(QuickPlayContinue(SUDOKU, HARD), playMix("a", TAKUZU)), state)
    }

    @Test
    fun `last started mix wins over earlier mixes`() {
        assertEquals(playMix("b", TAKUZU), widgetMix(listOf(mix("a", "sudoku"), mix("b", "takuzu")), "b"))
    }

    @Test
    fun `missing last started mix falls back to first playable mix`() {
        assertEquals(playMix("a", SUDOKU), widgetMix(listOf(mix("a", "sudoku"), mix("b", "takuzu")), "gone"))
    }

    @Test
    fun `mix with only beta games is skipped while beta is off`() {
        val mixes = listOf(mix("beta", "nback"), mix("b", "takuzu"))
        assertEquals(playMix("b", TAKUZU), widgetMix(mixes, "beta"))
        assertEquals(playMix("beta", NBACK), widgetMix(mixes, "beta", betaGamesEnabled = true))
    }

    @Test
    fun `mix games are its enabled games, heaviest first, at most three`() {
        val entries = listOf(
            MixEntry("sudoku", MixEntryMode.PUZZLE, "easy", 1),
            MixEntry("nback", MixEntryMode.PUZZLE, "easy", 9),
            MixEntry("takuzu", MixEntryMode.PUZZLE, "easy", 3),
            MixEntry("sudoku", MixEntryMode.PUZZLE, "hard", 4),
            MixEntry("nonogram", MixEntryMode.PUZZLE, "easy", 2),
            MixEntry("wordsearch", MixEntryMode.PUZZLE, "easy", 1),
        )
        assertEquals(QuickPlayMix("m", "Morning", listOf(SUDOKU, TAKUZU, NONOGRAM)), widgetMix(listOf(Mix("m", "Morning", entries)), "m"))
    }

    @Test
    fun `no mixes or only empty mixes leaves the mix widget without a mix`() {
        assertNull(widgetMix(emptyList(), null))
        assertNull(widgetMix(listOf(mix("empty")), "empty"))
    }

    @Test
    fun `games ranked by play count with most recent breaking ties`() {
        val history = listOf(
            record("sudoku", "easy", 1), record("sudoku", "easy", 2), record("sudoku", "easy", 3),
            record("takuzu", "easy", 10), record("takuzu", "easy", 20),
            record("nonogram", "easy", 15), record("nonogram", "easy", 30),
        )
        assertEquals(games(SUDOKU to EASY, NONOGRAM to EASY, TAKUZU to EASY), rankedGames(history))
    }

    @Test
    fun `each game uses its most played difficulty with most recent breaking ties`() {
        val history = listOf(record("sudoku", "hard", 1), record("sudoku", "hard", 2), record("sudoku", "easy", 3), record("sudoku", "easy", 4))
        assertEquals(games(SUDOKU to EASY, WORDSEARCH to EASY, MINESWEEPER to EASY), rankedGames(history))
    }

    @Test
    fun `challenger runs, unknown difficulties and unknown games are not counted`() {
        val history = List(5) { record("takuzu", "easy", it.toLong(), isChallenger = true) } +
            List(5) { record("blockfill", "endless", it.toLong()) } +
            List(5) { record("flowfree", "easy", it.toLong()) } +
            record("minesweeper", "hard", 1)
        assertEquals(games(MINESWEEPER to HARD, SUDOKU to EASY, WORDSEARCH to EASY), rankedGames(history))
    }

    @Test
    fun `beta games count only when beta is enabled`() {
        val history = List(3) { record("nback", "easy", it.toLong()) }
        assertEquals(defaults, rankedGames(history))
        assertEquals(games(NBACK to EASY, SUDOKU to EASY, WORDSEARCH to EASY), rankedGames(history, betaGamesEnabled = true))
    }

    @Test
    fun `fewer than three games are filled with defaults skipping duplicates`() {
        assertEquals(games(WORDSEARCH to MEDIUM, SUDOKU to EASY, MINESWEEPER to EASY), rankedGames(listOf(record("wordsearch", "medium", 1))))
    }

    @Test
    fun `no history gives the three defaults at easy`() {
        assertEquals(defaults, rankedGames(emptyList()))
    }

    @Test
    fun `favorites come before more played games, ranked among themselves by play count`() {
        val history = List(5) { record("sudoku", "easy", it.toLong()) } +
            List(2) { record("takuzu", "hard", 10 + it.toLong()) } +
            record("nonogram", "medium", 20)
        assertEquals(games(TAKUZU to HARD, NONOGRAM to MEDIUM, SUDOKU to EASY), rankedGames(history, favoriteGameIds = setOf("nonogram", "takuzu")))
    }

    @Test
    fun `unplayed favorites come at easy before played games`() {
        val history = List(3) { record("sudoku", "hard", it.toLong()) }
        assertEquals(games(STARBATTLE to EASY, SUDOKU to HARD, WORDSEARCH to EASY), rankedGames(history, favoriteGameIds = setOf("starbattle")))
    }

    @Test
    fun `beta favorites count only when beta is enabled`() {
        assertEquals(defaults, rankedGames(emptyList(), favoriteGameIds = setOf("nback")))
        assertEquals(games(NBACK to EASY, SUDOKU to EASY, WORDSEARCH to EASY), rankedGames(emptyList(), true, setOf("nback")))
    }
}
