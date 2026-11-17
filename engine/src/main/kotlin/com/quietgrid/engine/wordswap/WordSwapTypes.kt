package com.quietgrid.engine.wordswap

import kotlinx.serialization.Serializable

const val WORDSWAP_HOLE = '.'
const val WORDSWAP_MIN_WORD_LENGTH = 3

@Serializable
enum class WordSwapDirection(val rowStep: Int, val colStep: Int) {
    ACROSS(0, 1),
    DOWN(1, 0),
    DIAGONAL(1, 1),
    ANTI_DIAGONAL(1, -1),
}

val WordSwapDirection.isDiagonal: Boolean
    get() = this == WordSwapDirection.DIAGONAL || this == WordSwapDirection.ANTI_DIAGONAL

val WordSwapDirection.wordGroup: WordSwapDirection
    get() = if (isDiagonal) WordSwapDirection.DIAGONAL else this

@Serializable
data class WordSwapSlot(
    val row: Int,
    val col: Int,
    val length: Int,
    val direction: WordSwapDirection,
)

@Serializable
data class WordSwapPuzzleEntry(
    val id: String,
    val difficulty: String,
    val rows: Int,
    val cols: Int,
    val solution: String,
    val start: String,
    val slots: List<WordSwapSlot>,
    val minSwaps: Int,
    val rating: Int? = null,
)

data class WordSwapMove(val first: Int, val second: Int)
