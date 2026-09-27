package com.quietgrid.app.games.themeclear

import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import kotlinx.serialization.Serializable

@Serializable
data class TCTile(val id: Int, val letter: Char)

@Serializable
data class TCFoundWord(val word: String, val tiles: List<TCTile>)

data class TCTilePosition(val row: Int, val col: Int)

data class ThemeClearSession(
    val puzzle: ThemeClearPuzzleEntry,
    val columns: List<List<TCTile>>,
    val foundWords: List<TCFoundWord>,
    val selection: List<Int>,
    val hintsUsed: Int,
    val discoveredWords: Set<String> = emptySet(),
)

@Serializable
data class ThemeClearPersistedSession(
    val puzzle: ThemeClearPuzzleEntry,
    val columns: List<List<TCTile>>,
    val foundWords: List<TCFoundWord>,
    val hintsUsed: Int,
    val discoveredWords: Set<String> = emptySet(),
)
