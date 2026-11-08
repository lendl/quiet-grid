package com.quietgrid.cli.blockescape

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import com.quietgrid.engine.blockescape.gradeBlockEscape
import com.quietgrid.engine.blockescape.blockEscapeLayoutKey
import com.quietgrid.engine.blockescape.blockEscapeMinMoves
import com.quietgrid.engine.blockescape.isValidBlockEscapeLayout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BlockEscapeBankVerificationTest {
    private val bank: List<BlockEscapePuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(BlockEscapePuzzleEntry.serializer()), File("app/src/main/assets/blockescape_puzzles.json").readText())

    @Test
    fun `every tier has at least 100 puzzles`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(difficulty.key, bank.count { it.difficulty == difficulty.key } >= 100)
        }
    }

    @Test
    fun `ids and layouts are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertEquals(bank.size, bank.map { blockEscapeLayoutKey(it.blocks) }.toSet().size)
    }

    @Test
    fun `every puzzle is valid, needs its stored move count and sits in its tier`() {
        bank.forEach { entry ->
            assertTrue(entry.id, isValidBlockEscapeLayout(entry.blocks))
            assertEquals(entry.id, entry.minMoves, blockEscapeMinMoves(entry.blocks))
            assertEquals(entry.id, entry.difficulty, gradeBlockEscape(entry.minMoves)?.key)
            assertEquals(entry.id, blockEscapePuzzleId(entry.blocks), entry.id)
        }
    }
}
