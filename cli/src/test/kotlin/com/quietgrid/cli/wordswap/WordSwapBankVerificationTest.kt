package com.quietgrid.cli.wordswap

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.wordguess.loadWordGuessBlocklist
import com.quietgrid.engine.wordswap.WordSwapHint
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.findWordSwapHint
import com.quietgrid.engine.wordswap.countWordSwapArrangements
import com.quietgrid.engine.wordswap.gradeWordSwapPuzzle
import com.quietgrid.engine.wordswap.isValidWordSwapBoard
import com.quietgrid.engine.wordswap.read
import com.quietgrid.engine.wordswap.wordSwapCoversAllTiles
import com.quietgrid.engine.wordswap.wordSwapMinSwaps
import com.quietgrid.engine.wordswap.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSwapBankVerificationTest {

    private val bank: List<WordSwapPuzzleEntry> =
        readPuzzleEntries("app/src/main/assets/wordswap_puzzles.json", WordSwapPuzzleEntry.serializer())

    @Test
    fun everyTierHasPuzzles() {
        val counts = bank.groupingBy { it.difficulty }.eachCount()
        listOf("easy", "medium", "hard", "expert").forEach { tier ->
            assertTrue("$tier has ${counts[tier]} puzzles", (counts[tier] ?: 0) >= 120)
        }
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun hintFromStartIsAPlacementMatchingTheSolution() {
        bank.forEach { puzzle ->
            val hint = findWordSwapHint(puzzle, puzzle.start) as WordSwapHint.Placement
            assertEquals(puzzle.id, puzzle.words()[hint.slotIndex], hint.word)
        }
    }

    @Test
    fun everyPuzzleSolvesAtItsStoredTier() {
        bank.forEach { puzzle -> assertEquals(puzzle.id, puzzle.difficulty, gradeWordSwapPuzzle(puzzle)?.key) }
    }

    @Test
    fun everyPuzzleIsWellFormedAndHasOneAnswer() {
        bank.forEach { puzzle ->
            assertTrue(puzzle.id, isValidWordSwapBoard(puzzle, puzzle.start))
            assertTrue(puzzle.id, wordSwapCoversAllTiles(puzzle.solution, puzzle.cols, puzzle.slots))
            val words = puzzle.words()
            assertEquals(puzzle.id, words.size, words.toSet().size)
            assertEquals(puzzle.id, 1, countWordSwapArrangements(puzzle.solution, puzzle.cols, puzzle.slots, words))
        }
    }

    @Test
    fun storedFewestSwapsIsExactAndNoWordStartsInPlace() {
        bank.forEach { puzzle ->
            assertEquals(puzzle.id, wordSwapMinSwaps(puzzle.start, puzzle.solution), puzzle.minSwaps)
            assertTrue(puzzle.id, puzzle.slots.none { it.read(puzzle.start, puzzle.cols) == it.read(puzzle.solution, puzzle.cols) })
        }
    }

    @Test
    fun noBlockedWords() {
        val blocked = loadWordGuessBlocklist("en") + loadWordSwapBlocklistForTest()
        bank.forEach { puzzle ->
            puzzle.words().forEach { word -> assertTrue("${puzzle.id}: $word", word !in blocked) }
        }
    }

    private fun loadWordSwapBlocklistForTest(): Set<String> =
        javaClass.getResourceAsStream("/wordswap_blocklist_en.txt")!!.bufferedReader().useLines { lines ->
            lines.filter { !it.startsWith("#") }.flatMap { it.trim().split(Regex("\\s+")) }.filter { it.isNotEmpty() }.toSet()
        }
}
