package com.quietgrid.app.widget

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.Difficulty.EASY
import com.quietgrid.app.core.Difficulty.HARD
import com.quietgrid.app.core.Difficulty.MEDIUM
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.GameId.MINESWEEPER
import com.quietgrid.app.core.GameId.NBACK
import com.quietgrid.app.core.GameId.NONOGRAM
import com.quietgrid.app.core.GameId.SUDOKU
import com.quietgrid.app.core.GameId.TAKUZU
import com.quietgrid.app.core.GameId.WORDSEARCH
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.PlayRecord
import org.junit.Assert.assertEquals
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

    private fun mix(id: String, vararg games: String) = Mix(id, id, games.map { MixEntry(it, MixEntryMode.PUZZLE, "easy", 1) })

    private fun games(vararg pairs: Pair<GameId, Difficulty>) = QuickPlayState.Games(pairs.map { QuickPlayGame(it.first, it.second) })

    private val defaults = games(SUDOKU to EASY, WORDSEARCH to EASY, MINESWEEPER to EASY)

    @Test
    fun `active session shows Continue even when mixes and history exist`() {
        assertEquals(
            QuickPlayState.Continue,
            quickPlayState(ActiveSessionSummary("sudoku", null), listOf(mix("a", "takuzu")), "a", listOf(record("takuzu", "easy", 1)), false),
        )
    }

    @Test
    fun `active session for an unknown game is ignored`() {
        assertEquals(defaults, quickPlayState(ActiveSessionSummary("flowfree", null), emptyList(), null, emptyList(), false))
    }

    @Test
    fun `last started mix wins over earlier mixes`() {
        assertEquals(QuickPlayState.PlayMix("b"), quickPlayState(null, listOf(mix("a", "sudoku"), mix("b", "takuzu")), "b", emptyList(), false))
    }

    @Test
    fun `missing last started mix falls back to first playable mix`() {
        assertEquals(QuickPlayState.PlayMix("a"), quickPlayState(null, listOf(mix("a", "sudoku"), mix("b", "takuzu")), "gone", emptyList(), false))
    }

    @Test
    fun `mix with only beta games is skipped while beta is off`() {
        val mixes = listOf(mix("beta", "nback"), mix("b", "takuzu"))
        assertEquals(QuickPlayState.PlayMix("b"), quickPlayState(null, mixes, "beta", emptyList(), false))
        assertEquals(QuickPlayState.PlayMix("beta"), quickPlayState(null, mixes, "beta", emptyList(), true))
    }

    @Test
    fun `mix with no entries is not playable`() {
        assertEquals(defaults, quickPlayState(null, listOf(mix("empty")), "empty", emptyList(), false))
    }

    @Test
    fun `games ranked by play count with most recent breaking ties`() {
        val history = listOf(
            record("sudoku", "easy", 1), record("sudoku", "easy", 2), record("sudoku", "easy", 3),
            record("takuzu", "easy", 10), record("takuzu", "easy", 20),
            record("nonogram", "easy", 15), record("nonogram", "easy", 30),
        )
        assertEquals(games(SUDOKU to EASY, NONOGRAM to EASY, TAKUZU to EASY), quickPlayState(null, emptyList(), null, history, false))
    }

    @Test
    fun `each game uses its most played difficulty with most recent breaking ties`() {
        val history = listOf(record("sudoku", "hard", 1), record("sudoku", "hard", 2), record("sudoku", "easy", 3), record("sudoku", "easy", 4))
        assertEquals(games(SUDOKU to EASY, WORDSEARCH to EASY, MINESWEEPER to EASY), quickPlayState(null, emptyList(), null, history, false))
    }

    @Test
    fun `challenger runs, unknown difficulties and unknown games are not counted`() {
        val history = List(5) { record("takuzu", "easy", it.toLong(), isChallenger = true) } +
            List(5) { record("blockfill", "endless", it.toLong()) } +
            List(5) { record("flowfree", "easy", it.toLong()) } +
            record("minesweeper", "hard", 1)
        assertEquals(games(MINESWEEPER to HARD, SUDOKU to EASY, WORDSEARCH to EASY), quickPlayState(null, emptyList(), null, history, false))
    }

    @Test
    fun `beta games count only when beta is enabled`() {
        val history = List(3) { record("nback", "easy", it.toLong()) }
        assertEquals(defaults, quickPlayState(null, emptyList(), null, history, false))
        assertEquals(games(NBACK to EASY, SUDOKU to EASY, WORDSEARCH to EASY), quickPlayState(null, emptyList(), null, history, true))
    }

    @Test
    fun `fewer than three games are filled with defaults skipping duplicates`() {
        assertEquals(
            games(WORDSEARCH to MEDIUM, SUDOKU to EASY, MINESWEEPER to EASY),
            quickPlayState(null, emptyList(), null, listOf(record("wordsearch", "medium", 1)), false),
        )
    }

    @Test
    fun `no history and no mix gives the three defaults at easy`() {
        assertEquals(defaults, quickPlayState(null, emptyList(), null, emptyList(), false))
    }
}
