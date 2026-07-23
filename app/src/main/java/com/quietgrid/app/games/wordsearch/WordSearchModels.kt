package com.quietgrid.app.games.wordsearch

import kotlinx.serialization.Serializable

@Serializable
data class WSCellRef(val row: Int, val col: Int)

@Serializable
data class WSWordEntry(val id: String, val word: String, val positions: List<WSCellRef>)

@Serializable
data class WSHiddenWord(val word: String, val clue: String, val positions: List<WSCellRef>)

@Serializable
data class WordSearchPuzzleEntry(
    val id: String,
    val difficulty: String,
    val rows: Int,
    val cols: Int,
    val themeId: String,
    val grid: List<List<String>>,
    val words: List<WSWordEntry>,
    val hiddenWord: WSHiddenWord,
)

data class WSSelection(val path: List<WSCellRef>)

data class WordSearchSession(
    val puzzle: WordSearchPuzzleEntry,
    val foundWordIds: List<String>,
    val tempSelection: WSSelection?,
    val hiddenWordMode: Boolean,
    val hiddenWordProgress: List<WSCellRef>,
    val hiddenWordSolved: Boolean,
)

@Serializable
data class WordSearchPersistedSession(
    val puzzle: WordSearchPuzzleEntry,
    val foundWordIds: List<String>,
    val hiddenWordMode: Boolean,
    val hiddenWordProgress: List<WSCellRef>,
    val hiddenWordSolved: Boolean,
)
