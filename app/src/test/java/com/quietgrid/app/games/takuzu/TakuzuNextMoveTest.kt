package com.quietgrid.app.games.takuzu

import com.quietgrid.engine.takuzu.TakuzuGrid
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.decodePuzzleBoard
import com.quietgrid.engine.takuzu.decodeSolution
import com.quietgrid.engine.takuzu.findAvoidTriosMove
import com.quietgrid.engine.takuzu.findCompleteLinesMove
import com.quietgrid.engine.takuzu.findEliminateFilledLinesMove
import com.quietgrid.engine.takuzu.findPairsMove
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val DUMMY_SOLUTION: TakuzuGrid = listOf(
    listOf(0, 1, 0, 1),
    listOf(1, 0, 1, 0),
    listOf(0, 1, 1, 0),
    listOf(1, 0, 0, 1),
)

class TakuzuNextMoveTest {

    @Test
    fun `three consecutive equal cells in a row is a triple-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 0, 0, null),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )

        val hint = getTakuzuNextMoveHint(board, DUMMY_SOLUTION)

        assertTrue(hint is TakuzuNextMoveHint.AvoidTriosRepair)
        val repair = hint as TakuzuNextMoveHint.AvoidTriosRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.lineIndex)
        assertEquals(0, repair.repeatedValue)
    }

    @Test
    fun `more than half a row filled with the same value is a balance-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 1, 0, 0),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )

        val hint = getTakuzuNextMoveHint(board, DUMMY_SOLUTION)

        assertTrue(hint is TakuzuNextMoveHint.CompleteLinesRepair)
        val repair = hint as TakuzuNextMoveHint.CompleteLinesRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.lineIndex)
        assertEquals(0, repair.filledValue)
        assertEquals(3, repair.filledCount)
    }

    @Test
    fun `two identical completed rows is a duplicate-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 1, 0, 1),
            listOf(null, null, null, null),
            listOf(0, 1, 0, 1),
            listOf(null, null, null, null),
        )

        val hint = getTakuzuNextMoveHint(board, DUMMY_SOLUTION)

        assertTrue(hint is TakuzuNextMoveHint.EliminateFilledLinesRepair)
        val repair = hint as TakuzuNextMoveHint.EliminateFilledLinesRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.firstLineIndex)
        assertEquals(2, repair.secondLineIndex)
    }

    @Test
    fun `a pair of equal cells forcing the next one is a find-pairs progress hint`() {
        val board = listOf(
            listOf(0, 0, null, null),
            listOf(1, 0, 1, 0),
            listOf(1, 1, 0, 0),
            listOf(0, 1, 0, 1),
        )

        val hint = getTakuzuNextMoveHint(board, DUMMY_SOLUTION)

        assertTrue(hint is TakuzuNextMoveHint.FindPairs)
        assertEquals(TakuzuLineKind.ROW, (hint as TakuzuNextMoveHint.FindPairs).lineKind)
        assertEquals(0, hint.lineIndex)
    }

    @Test
    fun `a fully solved valid board is paused with no hint`() {
        val board = listOf(
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 1, 0),
            listOf(1, 0, 0, 1),
        )

        assertEquals(TakuzuNextMoveHint.Paused, getTakuzuNextMoveHint(board, DUMMY_SOLUTION))
    }

    @Test
    fun `a board where every cheap technique stalls falls back to a correct reveal from the solution`() {
        val json = Json { ignoreUnknownKeys = true }
        val entries = json.decodeFromString<List<TakuzuPuzzleEntry>>(
            File("src/main/assets/takuzu_puzzles.json").readText(),
        )

        for (entry in entries.filter { it.difficulty == "expert" || it.difficulty == "hard" }) {
            val solution = decodeSolution(entry.solution, entry.size)
            var board: TakuzuGrid = decodePuzzleBoard(entry.solution, entry.mask, entry.size)

            while (true) {
                val move = findPairsMove(board) ?: findAvoidTriosMove(board) ?: findCompleteLinesMove(board) ?: findEliminateFilledLinesMove(board) ?: break
                val next = board.map { it.toMutableList() }
                next[move.row][move.col] = move.value
                board = next
            }

            if (board.none { row -> row.any { it == null } }) continue

            val hint = getTakuzuNextMoveHint(board, solution)

            assertTrue("expected RevealFromSolution hint, got $hint", hint is TakuzuNextMoveHint.RevealFromSolution)
            val reveal = hint as TakuzuNextMoveHint.RevealFromSolution
            assertEquals(null, board[reveal.row][reveal.col])
            assertEquals(solution[reveal.row][reveal.col], reveal.value)
            return
        }

        throw AssertionError("no bundled expert/hard takuzu puzzle stalls on the cheap techniques; test fixture needs a different puzzle")
    }
}
