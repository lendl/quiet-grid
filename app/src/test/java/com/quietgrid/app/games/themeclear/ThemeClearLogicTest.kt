package com.quietgrid.app.games.themeclear

import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ThemeClearLogicTest {
    private val puzzle = ThemeClearPuzzleEntry(
        id = "tc-test",
        difficulty = "easy",
        themeId = "animals",
        rows = 2,
        cols = 3,
        grid = listOf("CAT", "DOG"),
        words = listOf("CAT", "DOG"),
    )

    private fun select(session: ThemeClearSession, vararg ids: Int) = ids.fold(session) { acc, id -> tcToggleTile(acc, id) }

    @Test
    fun `initial columns are bottom first with row major ids`() {
        val columns = themeClearInitialColumns(puzzle)
        assertEquals(listOf(TCTile(3, 'D'), TCTile(0, 'C')), columns[0])
        assertEquals(TCTilePosition(0, 0), tcTilePositions(themeClearNewSession(puzzle))[0])
        assertEquals(TCTilePosition(1, 0), tcTilePositions(themeClearNewSession(puzzle))[3])
    }

    @Test
    fun `toggling builds the word in tap order and toggling again removes a letter`() {
        val session = select(themeClearNewSession(puzzle), 2, 1, 0)
        assertEquals("TAC", tcSelectedWord(session))
        assertEquals("TC", tcSelectedWord(tcToggleTile(session, 1)))
        assertEquals("DOG", tcUnselectedLetters(session))
    }

    @Test
    fun `accepting the bottom row makes the top row fall`() {
        val accepted = tcAcceptSelection(select(themeClearNewSession(puzzle), 3, 4, 5))
        assertEquals(listOf(TCFoundWord("DOG", listOf(TCTile(3, 'D'), TCTile(4, 'O'), TCTile(5, 'G')))), accepted.foundWords)
        assertEquals(TCTilePosition(1, 0), tcTilePositions(accepted)[0])
        assertEquals(emptyList<Int>(), accepted.selection)
    }

    @Test
    fun `clearing every word empties the board`() {
        val first = tcAcceptSelection(select(themeClearNewSession(puzzle), 0, 1, 2))
        val second = tcAcceptSelection(select(first, 3, 4, 5))
        assertTrue(tcIsCleared(second))
        assertFalse(tcIsCleared(first))
    }

    @Test
    fun `removing a found word puts every letter back and always fits`() {
        val accepted = tcAcceptSelection(select(themeClearNewSession(puzzle), 3, 4, 5))
        val restored = tcRemoveFoundWord(accepted, 0, Random(1))
        assertEquals(listOf(2, 2, 2), restored.columns.map { it.size })
        assertEquals("ACDGOT", tcBoardLetters(restored).toList().sorted().joinToString(""))
        assertTrue(restored.foundWords.isEmpty())
    }

    @Test
    fun `shuffle keeps letters and packs them into rows from the bottom left`() {
        val accepted = tcAcceptSelection(select(themeClearNewSession(puzzle), 0))
        val shuffled = tcShuffle(accepted, Random(5))
        val count = tcBoardLetters(accepted).length
        val cols = puzzle.cols
        assertEquals(List(cols) { count / cols + if (it < count % cols) 1 else 0 }, shuffled.columns.map { it.size })
        assertEquals(tcBoardLetters(accepted).toList().sorted(), tcBoardLetters(shuffled).toList().sorted())
    }

    @Test
    fun `tiles for a word pick distinct board tiles`() {
        assertEquals(listOf(2, 1, 5), tcTilesForWord(themeClearNewSession(puzzle), "TAG"))
    }

    @Test
    fun `fresh session has no meaningful progress until something changes`() {
        val fresh = themeClearNewSession(puzzle)
        assertFalse(themeClearHasMeaningfulProgress(fresh))
        assertTrue(themeClearHasMeaningfulProgress(tcAcceptSelection(select(fresh, 0, 1, 2))))
    }

    @Test
    fun `score drops with hints and time but never below zero`() {
        assertEquals(1000, themeClearScore(hintsUsed = 0, elapsedSeconds = 0, discoveredWords = emptySet()))
        assertEquals(700, themeClearScore(hintsUsed = 2, elapsedSeconds = 50, discoveredWords = emptySet()))
        assertEquals(0, themeClearScore(hintsUsed = 20, elapsedSeconds = 0, discoveredWords = emptySet()))
    }

    @Test
    fun `every distinct discovered word adds ten points per letter`() {
        assertEquals(1060, themeClearScore(hintsUsed = 0, elapsedSeconds = 0, discoveredWords = setOf("CAT", "DOG")))
        assertEquals(30, themeClearScore(hintsUsed = 20, elapsedSeconds = 0, discoveredWords = setOf("CAT")))
    }

    @Test
    fun `supported locale is used as-is and unsupported locale falls back to english`() {
        assertEquals("nl", currentThemeClearLocale("nl"))
        assertEquals("de", currentThemeClearLocale("de"))
        assertEquals("en", currentThemeClearLocale("it"))
    }

    @Test
    fun `taking a word back keeps its discovery and finding it again does not count twice`() {
        val found = tcAcceptSelection(select(themeClearNewSession(puzzle), 0, 1, 2))
        val removed = tcRemoveFoundWord(found, 0, Random(2))
        assertEquals(setOf("CAT"), removed.discoveredWords)
        val positions = tcTilePositions(removed)
        val catIds = listOf('C', 'A', 'T').map { letter -> removed.columns.flatten().first { it.letter == letter && it.id in positions }.id }
        val refound = tcAcceptSelection(select(removed, *catIds.toIntArray()))
        assertEquals(setOf("CAT"), refound.discoveredWords)
    }
}
