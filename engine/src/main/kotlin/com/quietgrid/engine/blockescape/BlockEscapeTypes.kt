package com.quietgrid.engine.blockescape

import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.Serializable

const val BLOCKESCAPE_SIZE = 6
const val BLOCKESCAPE_EXIT_ROW = 2
const val BLOCKESCAPE_KEY_BLOCK = 0

@Serializable
data class BlockEscapeBlock(
    val row: Int,
    val col: Int,
    val length: Int,
    val horizontal: Boolean,
)

@Serializable
data class BlockEscapePuzzleEntry(
    val id: String,
    val difficulty: String,
    val blocks: List<BlockEscapeBlock>,
    val minMoves: Int,
    val rating: Int? = null,
)

@Serializable
data class BlockEscapeMove(val block: Int, val to: Int)

val BLOCKESCAPE_MOVES_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 4..8,
    Difficulty.MEDIUM to 8..13,
    Difficulty.HARD to 18..26,
    Difficulty.EXPERT to 30..99,
)

val BLOCKESCAPE_REPEAT_MOVES_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 0..0,
    Difficulty.MEDIUM to 0..4,
)

fun gradeBlockEscape(minMoves: Int, repeatMoves: () -> Int?): Difficulty? {
    val repeats by lazy(repeatMoves)
    return Difficulty.entries.firstOrNull { difficulty ->
        val repeatRange = BLOCKESCAPE_REPEAT_MOVES_BY_DIFFICULTY[difficulty]
        minMoves in BLOCKESCAPE_MOVES_BY_DIFFICULTY.getValue(difficulty) &&
            (repeatRange == null || repeats?.let { it in repeatRange } == true)
    }
}

fun gradeBlockEscape(blocks: List<BlockEscapeBlock>): Difficulty? {
    val minMoves = blockEscapeMinMoves(blocks) ?: return null
    return gradeBlockEscape(minMoves) { blockEscapeRepeatMoves(blocks) }
}
