package com.quietgrid.cli.wordswap

import com.quietgrid.cli.ASSETS_DIR
import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.wordguess.hunspellParadigmsContaining
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

    private val banks: Map<String, List<WordSwapPuzzleEntry>> = WORDSWAP_LOCALES.associateWith { locale ->
        readPuzzleEntries("$ASSETS_DIR/${wordSwapBankFile(locale)}", WordSwapPuzzleEntry.serializer())
    }

    private val plainWord = Regex("[a-z]{3,7}")

    @Test
    fun everyTierHasPuzzles() {
        banks.forEach { (locale, bank) ->
            val counts = bank.groupingBy { it.difficulty }.eachCount()
            listOf("easy", "medium", "hard", "expert").forEach { tier ->
                assertTrue("$locale $tier has ${counts[tier]} puzzles", (counts[tier] ?: 0) >= 120)
            }
            assertEquals(locale, bank.size, bank.map { it.id }.toSet().size)
        }
    }

    @Test
    fun hintFromStartIsAPlacementMatchingTheSolution() {
        banks.values.flatten().forEach { puzzle ->
            val hint = findWordSwapHint(puzzle, puzzle.start) as WordSwapHint.Placement
            assertEquals(puzzle.id, puzzle.words()[hint.slotIndex], hint.word)
        }
    }

    @Test
    fun everyPuzzleSolvesAtItsStoredTier() {
        banks.values.flatten().forEach { puzzle -> assertEquals(puzzle.id, puzzle.difficulty, gradeWordSwapPuzzle(puzzle)?.key) }
    }

    @Test
    fun everyPuzzleIsWellFormedAndHasOneAnswer() {
        banks.values.flatten().forEach { puzzle ->
            assertTrue(puzzle.id, isValidWordSwapBoard(puzzle, puzzle.start))
            assertTrue(puzzle.id, wordSwapCoversAllTiles(puzzle.solution, puzzle.cols, puzzle.slots))
            val words = puzzle.words()
            assertEquals(puzzle.id, words.size, words.toSet().size)
            assertEquals(puzzle.id, 1, countWordSwapArrangements(puzzle.solution, puzzle.cols, puzzle.slots, words))
        }
    }

    @Test
    fun storedFewestSwapsIsExactAndNoWordStartsInPlace() {
        banks.values.flatten().forEach { puzzle ->
            assertEquals(puzzle.id, wordSwapMinSwaps(puzzle.start, puzzle.solution), puzzle.minSwaps)
            assertTrue(puzzle.id, puzzle.slots.none { it.read(puzzle.start, puzzle.cols) == it.read(puzzle.solution, puzzle.cols) })
        }
    }

    @Test
    fun noBlockedWords() {
        banks.forEach { (locale, bank) ->
            val blocked = loadWordGuessBlocklist(locale) + loadWordSwapBlocklist(locale)
            bank.forEach { puzzle ->
                puzzle.words().forEach { word -> assertTrue("${puzzle.id}: $word", word !in blocked) }
            }
        }
    }

    @Test
    fun noWordIsAFormOfABlockedWord() {
        banks.forEach { (locale, bank) ->
            val blockedForms = hunspellParadigmsContaining(locale, loadWordGuessBlocklist(locale) + loadWordSwapBlocklist(locale))
            bank.forEach { puzzle ->
                puzzle.words().forEach { word -> assertTrue("${puzzle.id}: $word", word !in blockedForms) }
            }
        }
    }

    @Test
    fun idsAndLettersMatchTheirLocale() {
        banks.forEach { (locale, bank) ->
            val prefix = if (locale == "en") {
                Regex("ws-(easy|medium|hard|expert)-[0-9a-f]+")
            } else {
                Regex("ws-$locale-(easy|medium|hard|expert)-[0-9a-f]+")
            }
            bank.forEach { puzzle ->
                assertTrue(puzzle.id, prefix.matches(puzzle.id))
                assertTrue(puzzle.id, puzzle.words().all { plainWord.matches(it) })
            }
        }
    }
}
