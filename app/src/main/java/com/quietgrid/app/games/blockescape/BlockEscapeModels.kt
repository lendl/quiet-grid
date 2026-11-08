package com.quietgrid.app.games.blockescape

import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import kotlinx.serialization.Serializable

enum class BlockEscapeStatus { PLAYING, WON }

data class BlockEscapeSession(
    val puzzle: BlockEscapePuzzleEntry,
    val blocks: List<BlockEscapeBlock>,
    val moves: Int,
    val hintsUsed: Int,
    val status: BlockEscapeStatus,
)

@Serializable
data class BlockEscapePersistedSession(
    val puzzle: BlockEscapePuzzleEntry,
    val blocks: List<BlockEscapeBlock>,
    val moves: Int,
    val hintsUsed: Int,
    val status: String,
)

fun createBlockEscapeSession(puzzle: BlockEscapePuzzleEntry): BlockEscapeSession = BlockEscapeSession(
    puzzle = puzzle,
    blocks = puzzle.blocks,
    moves = 0,
    hintsUsed = 0,
    status = BlockEscapeStatus.PLAYING,
)
