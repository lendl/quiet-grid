package com.quietgrid.cli.battleship

import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import com.quietgrid.engine.battleship.countBattleshipSolutions
import com.quietgrid.engine.battleship.decodeBattleshipGivens
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BattleshipBankVerificationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadBank(): List<BattleshipPuzzleEntry> {
        val text = File("app/src/main/assets/battleship_puzzles.json").readText()
        return json.decodeFromString(text)
    }

    @Test
    fun `every shipped battleship puzzle has a unique solution`() {
        val bank = loadBank()
        assertTrue(bank.isNotEmpty())
        for (entry in bank) {
            val givens = decodeBattleshipGivens(entry.givens, entry.size).map { (row, col, cell) ->
                Triple(row, col, cell == com.quietgrid.engine.battleship.BattleshipCell.SHIP)
            }
            val count = countBattleshipSolutions(entry.size, entry.rowClues, entry.colClues, entry.fleet, cap = 2, givens = givens)
            assertEquals("puzzle ${entry.id} must have exactly one solution", 1, count)
        }
    }

    @Test
    fun `every shipped battleship puzzle id is unique`() {
        val bank = loadBank()
        val ids = bank.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every difficulty tier has at least one puzzle`() {
        val bank = loadBank()
        val difficulties = bank.map { it.difficulty }.toSet()
        assertEquals(setOf("easy", "medium", "hard", "expert"), difficulties)
    }
}
