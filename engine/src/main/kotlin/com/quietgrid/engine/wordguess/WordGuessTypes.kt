package com.quietgrid.engine.wordguess

import kotlinx.serialization.Serializable

@Serializable
data class WordGuessPuzzleEntry(
    val id: String,
    val locale: String,
    val difficulty: String,
    val word: String,
)

@Serializable
data class WordGuessDictionaryEntry(
    val locale: String,
    val word: String,
)
