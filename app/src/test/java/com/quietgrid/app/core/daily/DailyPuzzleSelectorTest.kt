package com.quietgrid.app.core.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class DailyPuzzleSelectorTest {

    private val pool = (1..50).map { "p${it.toString().padStart(3, '0')}" }
    private val start = LocalDate(2026, 9, 24)

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
        val picks = (0 until pool.size).map { selectDaily(pool, { it }, "sudoku", "hard", start.plus(it.toLong(), DateTimeUnit.DAY)) }
        assertEquals(pool.toSet(), picks.toSet())
    }

    @Test
    fun `cycle repeats after pool size days`() {
        val first = selectDaily(pool, { it }, "sudoku", "hard", start)
        val later = selectDaily(pool, { it }, "sudoku", "hard", start.plus(pool.size.toLong(), DateTimeUnit.DAY))
        assertEquals(first, later)
    }

    @Test
    fun `pick for a fixed date stays the same`() {
        val pool = (0 until 10).map { "p$it" }
        assertEquals("p1", selectDaily(pool, { it }, "takuzu", "easy", LocalDate(2026, 3, 4)))
    }

    @Test
    fun `dates before the epoch still resolve`() {
        val pick = selectDaily(pool, { it }, "sudoku", "hard", LocalDate(2025, 6, 1))
        assertEquals(true, pick in pool)
    }

    @Test
    fun `different tiers use different orders`() {
        val hard = (0 until 20).map { selectDaily(pool, { it }, "sudoku", "hard", start.plus(it.toLong(), DateTimeUnit.DAY)) }
        val easy = (0 until 20).map { selectDaily(pool, { it }, "sudoku", "easy", start.plus(it.toLong(), DateTimeUnit.DAY)) }
        assertNotEquals(hard, easy)
    }
}
