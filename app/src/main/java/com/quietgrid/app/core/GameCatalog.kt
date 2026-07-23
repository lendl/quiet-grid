package com.quietgrid.app.core

import androidx.annotation.StringRes
import com.quietgrid.app.R

enum class GameId(val key: String) {
    CHIMPTEST("chimptest"),
    TAKUZU("takuzu"),
    NONOGRAM("nonogram"),
    MINESWEEPER("minesweeper"),
    SUDOKU("sudoku"),
    WORDSEARCH("wordsearch"),
}

data class GameMeta(
    val id: GameId,
    @StringRes val titleRes: Int,
    @StringRes val taglineRes: Int,
    val beta: Boolean = false,
)

object GameCatalog {
    val games: List<GameMeta> = listOf(
        GameMeta(GameId.CHIMPTEST, R.string.chimp_title, R.string.chimp_tagline),
        GameMeta(GameId.TAKUZU, R.string.game_takuzu_name, R.string.takuzu_tagline),
        GameMeta(GameId.NONOGRAM, R.string.game_nonogram_name, R.string.nonogram_tagline, beta = true),
        GameMeta(GameId.MINESWEEPER, R.string.minesweeper_title, R.string.minesweeper_tagline),
        GameMeta(GameId.SUDOKU, R.string.sudoku_title, R.string.sudoku_tagline),
        GameMeta(GameId.WORDSEARCH, R.string.wordsearch_title, R.string.wordsearch_tagline),
    )

    fun get(id: GameId): GameMeta = games.first { it.id == id }
}
