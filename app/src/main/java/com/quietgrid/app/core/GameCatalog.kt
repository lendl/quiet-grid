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
    BLOCKFILL("blockfill"),
    WORDGUESS("wordguess"),
    ANIMALDOKU("animaldoku"),
    ARROWESCAPE("arrowescape"),
    GAME_2048("2048"),
    STARBATTLE("starbattle"),
    GUESSBYNUMBERS("guessbynumbers"),
    NBACK("nback"),
    FLOWFREE("flowfree"),
    BATTLESHIP("battleship"),
}

enum class GameCategory(@param:StringRes val labelRes: Int) {
    LOGIC(R.string.games_category_logic),
    WORD(R.string.games_category_word),
    MEMORY(R.string.games_category_memory),
    SPATIAL(R.string.games_category_spatial),
}

data class GameMeta(
    val id: GameId,
    @param:StringRes val titleRes: Int,
    @param:StringRes val taglineRes: Int,
    val categories: Set<GameCategory>,
    val beta: Boolean = false,
)

object GameCatalog {
    val games: List<GameMeta> = listOf(
        GameMeta(GameId.CHIMPTEST, R.string.chimp_title, R.string.chimp_tagline, setOf(GameCategory.MEMORY)),
        GameMeta(GameId.TAKUZU, R.string.game_takuzu_name, R.string.takuzu_tagline, setOf(GameCategory.LOGIC)),
        GameMeta(GameId.NONOGRAM, R.string.game_nonogram_name, R.string.nonogram_tagline, setOf(GameCategory.LOGIC)),
        GameMeta(GameId.MINESWEEPER, R.string.minesweeper_title, R.string.minesweeper_tagline, setOf(GameCategory.LOGIC)),
        GameMeta(GameId.SUDOKU, R.string.sudoku_title, R.string.sudoku_tagline, setOf(GameCategory.LOGIC)),
        GameMeta(GameId.WORDSEARCH, R.string.wordsearch_title, R.string.wordsearch_tagline, setOf(GameCategory.WORD)),
        GameMeta(GameId.BLOCKFILL, R.string.blockfill_title, R.string.blockfill_tagline, setOf(GameCategory.SPATIAL), beta = true),
        GameMeta(GameId.WORDGUESS, R.string.wordguess_title, R.string.wordguess_tagline, setOf(GameCategory.WORD)),
        GameMeta(GameId.ANIMALDOKU, R.string.animaldoku_title, R.string.animaldoku_tagline, setOf(GameCategory.LOGIC, GameCategory.SPATIAL)),
        GameMeta(GameId.ARROWESCAPE, R.string.arrowescape_title, R.string.arrowescape_tagline, setOf(GameCategory.SPATIAL), beta = true),
        GameMeta(GameId.GAME_2048, R.string.game2048_title, R.string.game2048_tagline, setOf(GameCategory.SPATIAL)),
        GameMeta(GameId.STARBATTLE, R.string.starbattle_title, R.string.starbattle_tagline, setOf(GameCategory.LOGIC, GameCategory.SPATIAL)),
        GameMeta(GameId.GUESSBYNUMBERS, R.string.guessbynumbers_title, R.string.guessbynumbers_tagline, setOf(GameCategory.WORD, GameCategory.LOGIC)),
        GameMeta(GameId.NBACK, R.string.nback_title, R.string.nback_tagline, setOf(GameCategory.MEMORY), beta = true),
        GameMeta(GameId.FLOWFREE, R.string.flowfree_title, R.string.flowfree_tagline, setOf(GameCategory.LOGIC, GameCategory.SPATIAL), beta = true),
        GameMeta(GameId.BATTLESHIP, R.string.battleship_title, R.string.battleship_tagline, setOf(GameCategory.LOGIC, GameCategory.SPATIAL), beta = true),
    )

    fun get(id: GameId): GameMeta = games.first { it.id == id }
}

val CHALLENGER_CAPABLE_GAMES: Set<GameId> = setOf(GameId.CHIMPTEST, GameId.WORDGUESS, GameId.ANIMALDOKU, GameId.STARBATTLE, GameId.SUDOKU, GameId.TAKUZU, GameId.NONOGRAM)
