package com.quietgrid.cli.battleship

import com.quietgrid.engine.battleship.BATTLESHIP_FLEET_BY_DIFFICULTY
import com.quietgrid.engine.battleship.BATTLESHIP_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import com.quietgrid.engine.battleship.BattleshipSolveProfile
import com.quietgrid.engine.battleship.analyzeBattleshipSolveResult
import com.quietgrid.engine.battleship.classifyBattleshipDifficulty
import com.quietgrid.engine.battleship.decodeBattleshipGivens
import com.quietgrid.engine.battleship.solveBattleship
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleshipPlaytestTest {
    private fun generateWithRetry(difficulty: Difficulty, attempts: Int = 50): BattleshipPuzzleEntry? {
        repeat(attempts) {
            val entry = generateBattleshipPuzzleForTier(
                BATTLESHIP_SIZE_BY_DIFFICULTY.getValue(difficulty),
                BATTLESHIP_FLEET_BY_DIFFICULTY.getValue(difficulty),
                difficulty,
                idPrefix = "playtest",
            )
            if (entry != null) return entry
        }
        return null
    }

    private fun profileFor(difficulty: Difficulty): BattleshipSolveProfile {
        val entry = generateWithRetry(difficulty)
        checkNotNull(entry) { "Could not generate a $difficulty puzzle for playtest within 50 attempts" }
        val givens = decodeBattleshipGivens(entry.givens, entry.size)
        val result = solveBattleship(entry.size, entry.rowClues, entry.colClues, entry.fleet, givens)
        assertEquals(difficulty, classifyBattleshipDifficulty(result))
        val profile = analyzeBattleshipSolveResult(result)
        val trace = result.steps.map { it.technique.name }
        println(
            "$difficulty trace: size=${entry.size} givens=${givens.size} steps=${result.steps.size} " +
                "probingCount=${profile.probingCount} flm=${profile.fleetLengthMatchCount} fe=${profile.fleetEliminationCount} " +
                "hardest=${profile.hardestTechnique} techniques=$trace",
        )
        return profile
    }

    @Test
    fun `easy trace is pure counting with zero probing`() {
        val profile = profileFor(Difficulty.EASY)
        assertEquals(0, profile.probingCount)
    }

    @Test
    fun `medium trace needs real reasoning beyond pure counting but stays shallow`() {
        val profile = profileFor(Difficulty.MEDIUM)
        assertTrue(profile.probingCount <= 2)
    }

    @Test
    fun `hard trace requires a moderate amount of probing`() {
        val profile = profileFor(Difficulty.HARD)
        assertTrue(profile.probingCount in 3..6)
    }

    @Test
    fun `expert trace requires heavy probing`() {
        val profile = profileFor(Difficulty.EXPERT)
        assertTrue(profile.probingCount > 6)
    }
}
