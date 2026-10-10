package com.quietgrid.cli.hashi

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import com.quietgrid.engine.hashi.classifyHashiDifficulty
import com.quietgrid.engine.hashi.countHashiSolutions
import com.quietgrid.engine.hashi.isHashiSolved
import com.quietgrid.engine.hashi.solveHashi
import com.quietgrid.engine.hashi.toBoard
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HashiBankVerificationTest {
    private val bank: List<HashiPuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(HashiPuzzleEntry.serializer()), File("app/src/main/assets/hashi_puzzles.json").readText())

    @Test
    fun `every tier has at least 100 puzzles`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(difficulty.key, bank.count { it.difficulty == difficulty.key } >= 100)
        }
    }

    @Test
    fun `ids and layouts are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertEquals(bank.size, bank.map { it.islands }.toSet().size)
    }

    @Test
    fun `every puzzle has one answer, matches its stored solution and its tier`() {
        bank.forEach { entry ->
            val board = entry.toBoard()
            assertTrue(entry.id, isHashiSolved(board, board.countsFromBridges(entry.solution)))
            assertEquals(entry.id, 1, countHashiSolutions(board))
            assertEquals(entry.id, entry.difficulty, classifyHashiDifficulty(solveHashi(board))?.key)
        }
    }
}
