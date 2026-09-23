package com.quietgrid.engine.battleship

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BattleshipDifficultyTest {
    @Test
    fun `classifyBattleshipDifficulty returns null for an unsolved trace`() {
        val result = BattleshipSolveResult(solved = false, steps = emptyList())
        assertNull(classifyBattleshipDifficulty(result))
    }

    @Test
    fun `classifyBattleshipDifficulty grades a purely counting trace as easy`() {
        val steps = listOf(
            BattleshipStep.Water(listOf(0 to 0), BattleshipTechnique.STRUCTURAL_WATER),
            BattleshipStep.Ship(listOf(1 to 1), BattleshipTechnique.LINE_EXHAUSTION),
        )
        val result = BattleshipSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.EASY, classifyBattleshipDifficulty(result))
    }

    @Test
    fun `classifyBattleshipDifficulty grades a trace with a fleet-length-match but no probing as medium`() {
        val steps = listOf(BattleshipStep.Ship(listOf(0 to 0), BattleshipTechnique.FLEET_LENGTH_MATCH))
        val result = BattleshipSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.MEDIUM, classifyBattleshipDifficulty(result))
    }

    @Test
    fun `classifyBattleshipDifficulty grades a trace with up to 2 probes as medium`() {
        val steps = List(2) { BattleshipStep.Water(listOf(it to it), BattleshipTechnique.PROBING) }
        val result = BattleshipSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.MEDIUM, classifyBattleshipDifficulty(result))
    }

    @Test
    fun `classifyBattleshipDifficulty grades a trace with 3 to 6 probes as hard`() {
        val steps = List(5) { BattleshipStep.Water(listOf(it to it), BattleshipTechnique.PROBING) }
        val result = BattleshipSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.HARD, classifyBattleshipDifficulty(result))
    }

    @Test
    fun `classifyBattleshipDifficulty grades a trace with more than 6 probes as expert`() {
        val steps = List(9) { BattleshipStep.Water(listOf(it to it), BattleshipTechnique.PROBING) }
        val result = BattleshipSolveResult(solved = true, steps = steps)
        assertEquals(Difficulty.EXPERT, classifyBattleshipDifficulty(result))
    }
}
