package com.quietgrid.app.games.wordsearch

import com.quietgrid.engine.wordsearch.WSCellRef
import com.quietgrid.engine.wordsearch.WSHiddenWord
import com.quietgrid.engine.wordsearch.WSWordEntry
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import kotlinx.serialization.Serializable

data class WSSelection(val path: List<WSCellRef>)

data class WordSearchSession(
    val puzzle: WordSearchPuzzleEntry,
    val foundWordIds: List<String>,
    val tempSelection: WSSelection?,
    val hiddenWordMode: Boolean,
    val hiddenWordProgress: List<WSCellRef>,
    val hiddenWordSolved: Boolean,
    val accuracyDrops: Int,
)

@Serializable
data class WordSearchPersistedSession(
    val puzzle: WordSearchPuzzleEntry,
    val foundWordIds: List<String>,
    val hiddenWordMode: Boolean,
    val hiddenWordProgress: List<WSCellRef>,
    val hiddenWordSolved: Boolean,
    val accuracyDrops: Int = 0,
)
