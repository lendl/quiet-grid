package com.quietgrid.cli.blockescape

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import com.quietgrid.engine.blockescape.generateBlockEscapePuzzle
import com.quietgrid.engine.blockescape.blockEscapeLayoutKey
import java.security.MessageDigest
import kotlin.random.Random

fun blockEscapePuzzleId(blocks: List<BlockEscapeBlock>): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(blockEscapeLayoutKey(blocks).toByteArray())
    return "be-" + digest.take(6).joinToString("") { "%02x".format(it) }
}

fun generateBlockEscapeEntry(difficulty: Difficulty, random: Random = Random.Default): BlockEscapePuzzleEntry? {
    val (blocks, minMoves) = generateBlockEscapePuzzle(difficulty, random) ?: return null
    return BlockEscapePuzzleEntry(
        id = blockEscapePuzzleId(blocks),
        difficulty = difficulty.key,
        blocks = blocks,
        minMoves = minMoves,
    )
}
