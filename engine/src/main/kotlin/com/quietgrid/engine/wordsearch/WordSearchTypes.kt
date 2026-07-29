package com.quietgrid.engine.wordsearch

import kotlinx.serialization.Serializable

enum class WordSearchDirection { RIGHT, LEFT, DOWN, UP, DOWN_RIGHT, DOWN_LEFT, UP_RIGHT, UP_LEFT }

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
