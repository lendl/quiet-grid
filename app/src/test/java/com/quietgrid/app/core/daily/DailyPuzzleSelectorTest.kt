package com.quietgrid.app.core.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyPuzzleSelectorTest {

    private val pool = (1..50).map { "p${it.toString().padStart(3, '0')}" }
    private val start = LocalDate.of(2026, 9, 24)

    @Test
    fun `fnv1a64 matches reference vectors`() {
        assertEquals(0xcbf29ce484222325uL.toLong(), fnv1a64(""))
        assertEquals(0xaf63dc4c8601ec8cuL.toLong(), fnv1a64("a"))
        assertEquals(0x369399aa1283bdc1uL.toLong(), fnv1a64("sudoku|hard"))
    }

    @Test
    fun `empty pool gives null`() {
        assertNull(selectDaily(emptyList<String>(), { it }, "sudoku", "hard", start))
    }

    @Test
    fun `same inputs give same pick`() {
        val a = selectDaily(pool, { it }, "sudoku", "hard", start)
        val b = selectDaily(pool, { it }, "sudoku", "hard", start)
        assertEquals(a, b)
    }

    @Test
    fun `pick is independent of input order`() {
        val a = selectDaily(pool, { it }, "sudoku", "hard", start)
        val b = selectDaily(pool.reversed(), { it }, "sudoku", "hard", start)
        assertEquals(a, b)
    }

    @Test
    fun `consecutive days cover every puzzle once per cycle`() {
        val picks = (0 until pool.size).map { selectDaily(pool, { it }, "sudoku", "hard", start.plusDays(it.toLong())) }
        assertEquals(pool.toSet(), picks.toSet())
    }

    @Test
    fun `cycle repeats after pool size days`() {
        val first = selectDaily(pool, { it }, "sudoku", "hard", start)
        val later = selectDaily(pool, { it }, "sudoku", "hard", start.plusDays(pool.size.toLong()))
        assertEquals(first, later)
    }

    @Test
    fun `dates before the epoch still resolve`() {
        val pick = selectDaily(pool, { it }, "sudoku", "hard", LocalDate.of(2025, 6, 1))
        assertEquals(true, pick in pool)
    }

    @Test
    fun `different tiers use different orders`() {
        val hard = (0 until 20).map { selectDaily(pool, { it }, "sudoku", "hard", start.plusDays(it.toLong())) }
        val easy = (0 until 20).map { selectDaily(pool, { it }, "sudoku", "easy", start.plusDays(it.toLong())) }
        assertNotEquals(hard, easy)
    }
}
