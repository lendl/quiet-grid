package com.quietgrid.cli.takuzu

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.takuzu.countSolutions
import com.quietgrid.engine.takuzu.decodePuzzleBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TakuzuGeneratorTest {
    @Test
    fun `generateTakuzuPuzzle produces a uniquely-solvable 6x6 easy puzzle`() {
        val entry = generateTakuzuPuzzle(size = 6, targetDifficulty = Difficulty.EASY, idPrefix = "t")
        assertNotNull(entry)
        assertEquals(6, entry!!.size)
        assertEquals("easy", entry.difficulty)
        val board = decodePuzzleBoard(entry.solution, entry.mask, entry.size)
        assertEquals(1, countSolutions(board))
    }

    @Test
    fun `generateTakuzuPuzzle rejects unsupported sizes`() {
        assertThrows(IllegalArgumentException::class.java) {
            generateTakuzuPuzzle(size = 7, targetDifficulty = Difficulty.EASY, idPrefix = "t")
        }
    }
}
