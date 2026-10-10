package com.quietgrid.cli.hashi

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.hashi.HashiBoard
import com.quietgrid.engine.hashi.classifyHashiDifficulty
import com.quietgrid.engine.hashi.countHashiSolutions
import com.quietgrid.engine.hashi.isHashiSolved
import com.quietgrid.engine.hashi.solveHashi
import com.quietgrid.engine.hashi.toBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HashiGeneratorTest {
    @Test
    fun `layouts keep islands apart and every count matches its bridges`() {
        val random = Random(11)
        repeat(50) {
            val layout = generateHashiLayout(hashiLayoutParamsFor(Difficulty.MEDIUM), random) ?: return@repeat
            val board = HashiBoard(layout.rows, layout.cols, layout.islands)
            assertTrue(isHashiSolved(board, board.countsFromBridges(layout.bridges)))
            val cells = layout.islands.map { it.row to it.col }.toSet()
            layout.islands.forEach { island ->
                assertTrue(listOf(0 to 1, 1 to 0).none { (dr, dc) -> (island.row + dr to island.col + dc) in cells })
            }
        }
    }

    @Test
    fun `the logic solver finishes exactly the layouts that have one answer`() {
        val random = Random(5)
        repeat(300) {
            val difficulty = Difficulty.entries[it % Difficulty.entries.size]
            val layout = generateHashiLayout(hashiLayoutParamsFor(difficulty), random) ?: return@repeat
            val board = HashiBoard(layout.rows, layout.cols, layout.islands)
            val solved = solveHashi(board)
            val unique = countHashiSolutions(board) == 1
            assertEquals(unique, solved.solved)
            if (solved.solved) assertTrue(isHashiSolved(board, solved.counts!!))
        }
    }

    @Test
    fun `generated puzzles land on the requested tier with a single answer`() {
        Difficulty.entries.forEach { difficulty ->
            val entry = generateHashiPuzzle(difficulty, Random(difficulty.ordinal + 1), maxAttempts = 50_000)!!
            val board = entry.toBoard()
            assertEquals(difficulty.key, entry.difficulty)
            assertEquals(difficulty, classifyHashiDifficulty(solveHashi(board)))
            assertEquals(1, countHashiSolutions(board))
            assertTrue(isHashiSolved(board, board.countsFromBridges(entry.solution)))
        }
    }
}
