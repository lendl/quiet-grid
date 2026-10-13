package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class GamesGridOrderTest {

    private val titles = mapOf(
        GameId.SUDOKU to "Sudoku",
        GameId.NONOGRAM to "Nonogram",
        GameId.TAKUZU to "Takuzu",
        GameId.GAME_2048 to "2048",
        GameId.WORDSEARCH to "Word Search",
    )

    private val games = titles.keys.map { GameCatalog.get(it) }

    @Test
    fun `without favorites games are sorted by title`() {
        assertEquals(
            listOf(GameId.GAME_2048, GameId.NONOGRAM, GameId.SUDOKU, GameId.TAKUZU, GameId.WORDSEARCH),
            gamesInGridOrder(games, titles, emptySet()).map { it.id },
        )
    }

    @Test
    fun `favorites come first, each group sorted by title`() {
        assertEquals(
            listOf(GameId.SUDOKU, GameId.WORDSEARCH, GameId.GAME_2048, GameId.NONOGRAM, GameId.TAKUZU),
            gamesInGridOrder(games, titles, setOf("wordsearch", "sudoku")).map { it.id },
        )
    }

    @Test
    fun `favorite keys for games not in the list are ignored`() {
        assertEquals(
            listOf(GameId.GAME_2048, GameId.NONOGRAM, GameId.SUDOKU, GameId.TAKUZU, GameId.WORDSEARCH),
            gamesInGridOrder(games, titles, setOf("hashi", "flowfree")).map { it.id },
        )
    }
}
