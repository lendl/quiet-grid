package com.quietgrid.engine.wordswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordSwapEngineTest {

    private fun bruteForceMinSwaps(start: String, target: String): Int {
        if (start == target) return 0
        val seen = hashSetOf(start)
        var frontier = listOf(start)
        var depth = 0
        while (frontier.isNotEmpty()) {
            depth++
            val next = mutableListOf<String>()
            for (board in frontier) {
                for (a in board.indices) for (b in a + 1 until board.length) {
                    if (board[a] == board[b]) continue
                    val moved = applyWordSwapMove(board, WordSwapMove(a, b)) ?: continue
                    if (moved == target) return depth
                    if (seen.add(moved)) next += moved
                }
            }
            frontier = next
        }
        error("unreachable")
    }

    @Test
    fun minSwapsMatchesBreadthFirstSearchWithRepeatedLetters() {
        val random = Random(7)
        repeat(300) {
            val length = 4 + random.nextInt(4)
            val target = String(CharArray(length) { 'a' + random.nextInt(3) })
            val start = target.toList().shuffled(random).joinToString("")
            assertEquals("$start -> $target", bruteForceMinSwaps(start, target), wordSwapMinSwaps(start, target))
        }
    }

    @Test
    fun minSwapsIgnoresHoles() {
        assertEquals(0, wordSwapMinSwaps("ab.c", "ab.c"))
        assertEquals(1, wordSwapMinSwaps("ba.c", "ab.c"))
        assertEquals(2, wordSwapMinSwaps("bca.", "abc."))
    }

    @Test
    fun hintAlwaysTakesOneSwapOffTheFewest() {
        val random = Random(11)
        repeat(200) {
            val length = 6 + random.nextInt(8)
            val target = String(CharArray(length) { 'a' + random.nextInt(5) })
            var board = target.toList().shuffled(random).joinToString("")
            var remaining = wordSwapMinSwaps(board, target)
            while (remaining > 0) {
                val hint = findWordSwapSwapHint(board, target)!!
                board = applyWordSwapMove(board, hint)!!
                val next = wordSwapMinSwaps(board, target)
                assertEquals(remaining - 1, next)
                remaining = next
            }
            assertEquals(target, board)
            assertNull(findWordSwapSwapHint(board, target))
        }
    }

    @Test
    fun lineSlotsSkipRunsShorterThanThree() {
        val layout = "aaaa" + "a..a" + "aaaa" + "aaaa"
        val slots = wordSwapLineSlots(layout, 4, 4)
        assertEquals(3, slots.count { it.direction == WordSwapDirection.ACROSS })
        assertEquals(2, slots.count { it.direction == WordSwapDirection.DOWN })
        assertTrue(wordSwapCoversAllTiles(layout, 4, slots))
    }

    @Test
    fun diagonalSlotsNeedAnOpenDiagonal() {
        val open = "aaa" + "a.a" + "aaa"
        assertNull(wordSwapDiagonalSlot(open, 3, WordSwapDirection.DIAGONAL))
        val full = "aaa" + "aaa" + "aaa"
        val anti = wordSwapDiagonalSlot(full, 3, WordSwapDirection.ANTI_DIAGONAL)!!
        assertEquals(listOf(2, 4, 6), anti.cells(3))
    }

    @Test
    fun arrangementCountFindsAmbiguousBoards() {
        val solution = "tap" + "a.a" + "pat"
        val slots = wordSwapLineSlots(solution, 3, 3)
        val words = slots.map { it.read(solution, 3) }
        assertEquals(listOf("tap", "pat", "tap", "pat"), words)
        assertEquals(2, countWordSwapArrangements(solution, 3, slots, words))
    }

    @Test
    fun arrangementCountKeepsWordsInTheirDirection() {
        val solution = "bad" + "a.o" + "tog"
        val slots = wordSwapLineSlots(solution, 3, 3)
        val words = slots.map { it.read(solution, 3) }
        assertEquals(listOf("bad", "tog", "bat", "dog"), words)
        assertEquals(1, countWordSwapArrangements(solution, 3, slots, words))
    }

    @Test
    fun swapsRejectHolesAndSameCell() {
        assertNull(applyWordSwapMove("ab.c", WordSwapMove(0, 2)))
        assertNull(applyWordSwapMove("abc", WordSwapMove(1, 1)))
        assertEquals("bac", applyWordSwapMove("abc", WordSwapMove(0, 1)))
    }

    @Test
    fun solvedSlotsTrackFinishedWords() {
        val solution = "bat" + "a.o" + "dog"
        val puzzle = WordSwapPuzzleEntry("t", "easy", 3, 3, solution, "bat" + "o.a" + "dgo", wordSwapLineSlots(solution, 3, 3), 1)
        val solved = wordSwapSolvedSlots(puzzle, puzzle.start)
        assertEquals(setOf(0), solved)
        assertFalse(isWordSwapSolved(puzzle, puzzle.start))
        assertTrue(isValidWordSwapBoard(puzzle, puzzle.start))
    }
}
