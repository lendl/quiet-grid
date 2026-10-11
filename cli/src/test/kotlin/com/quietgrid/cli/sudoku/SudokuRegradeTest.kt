package com.quietgrid.cli.sudoku

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SudokuRegradeTest {
    private val nakedSinglesOnly = ".64..8..98.127.64...24635.818659....295....614....1.5.5289.71...13.2.4..7..3162.5"
    private val oneForcedHiddenSingle = "12.8.7.454...9.....8.6..2..254...3..........4637..2.8....1.3..6.7..2.138..8.6..92"
    private val twoLockedCandidateStuckPoints = "58.9.26479.........7.5..2..193...8..6.......4247..831....8.9....2..6.981..9.2.576"
    private val blank = ".".repeat(81)

    private fun entry(id: String, difficulty: String, cells: String) = SudokuPuzzleEntry(
        id = id,
        difficulty = difficulty,
        givens = cells.chunked(9).map { row -> row.map { if (it == '.') null else it.digitToInt() } },
        solution = List(9) { List(9) { 0 } },
    )

    @Test
    fun `regrade relabels moves and removes in original order`() {
        val result = regradeSudokuEntries(
            listOf(
                entry("a", "medium", nakedSinglesOnly),
                entry("b", "medium", oneForcedHiddenSingle),
                entry("c", "hard", twoLockedCandidateStuckPoints),
                entry("d", "easy", blank),
            ),
        )

        assertEquals(listOf("a" to "easy", "c" to "hard"), result.kept.map { it.id to it.difficulty })
        assertEquals(listOf("a" to "easy", "b" to null, "d" to null), result.changes.map { it.id to it.to })
        assertNull(result.changes.single { it.id == "d" }.grade)
    }

    @Test
    fun `writePuzzleEntries round-trips through readPuzzleEntries`() {
        val file = File.createTempFile("sudoku-regrade", ".json")
        file.deleteOnExit()
        val entries = listOf(entry("a", "easy", nakedSinglesOnly), entry("c", "hard", twoLockedCandidateStuckPoints))

        writePuzzleEntries(file.path, entries, SudokuPuzzleEntry.serializer())

        assertEquals(entries, readPuzzleEntries(file.path, SudokuPuzzleEntry.serializer()))
        assertTrue(file.readText().startsWith("[{\"id\":"))
    }
}
