package com.quietgrid.cli.wordswap

import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.wordSwapLineSlots
import com.quietgrid.engine.wordswap.wordSwapMinSwaps
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSwapPruneTest {

    private fun puzzle(id: String, solution: String, start: String) = WordSwapPuzzleEntry(
        id = id,
        difficulty = "easy",
        rows = 3,
        cols = 3,
        solution = solution,
        start = start,
        slots = wordSwapLineSlots(solution, 3, 3),
        minSwaps = wordSwapMinSwaps(start, solution),
    )

    @Test
    fun dropsPuzzlesWithAWordOutsideTheAllowedList() {
        val keep = puzzle("keep", "bat" + "a.o" + "dog", "tab" + "o.a" + "dgo")
        val drop = puzzle("drop", "cat" + "a.o" + "dog", "tac" + "o.a" + "dgo")
        val allowed = setOf("bat", "dog", "bad", "tog")

        assertEquals(listOf(keep), pruneWordSwapBank(listOf(keep, drop), allowed))
    }
}
