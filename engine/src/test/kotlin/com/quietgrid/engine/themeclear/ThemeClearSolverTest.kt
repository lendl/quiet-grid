package com.quietgrid.engine.themeclear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeClearSolverTest {

    private fun solver(vararg words: String) = ThemeClearSolver(ThemeClearDictionary(words.toList()))

    @Test
    fun `findFinish splits letters into theme words`() {
        assertEquals(listOf("LION", "ZEBRA"), solver("LION", "ZEBRA").findFinish("ZLEIBORNA")?.sorted())
    }

    @Test
    fun `findFinish returns null when a letter can never be used`() {
        assertNull(solver("LION", "ZEBRA").findFinish("LIONZEBRAX"))
    }

    @Test
    fun `findFinish of no letters is an empty finish`() {
        assertEquals(emptyList<String>(), solver("LION").findFinish(""))
    }

    @Test
    fun `the same word may be used twice`() {
        assertEquals(listOf("CAT", "CAT"), solver("CAT").findFinish("CATCAT"))
    }

    @Test
    fun `anagram words count as separate finishes`() {
        assertEquals(2, solver("LEMON", "MELON").countFinishes("LEMON", cap = 10))
    }

    @Test
    fun `countFinishes stops at the cap`() {
        assertEquals(1, solver("LEMON", "MELON").countFinishes("LEMON", cap = 1))
    }

    @Test
    fun `analyze counts finishes, spellable words and trap words`() {
        val metrics = solver("LION", "PAN", "NAP", "OIL", "PLAN").analyze("LIONPAN")
        assertEquals(2, metrics.finishCount)
        assertEquals(2, metrics.trapWords)
        assertEquals(5, metrics.spellableWords)
    }

    @Test
    fun `blind success is certain when only one word fits`() {
        assertEquals(1.0, solver("LION").blindSuccessRate("NOIL"), 0.0)
    }

    @Test
    fun `blind success is zero when the letters cannot be cleared`() {
        assertEquals(0.0, solver("LION").blindSuccessRate("LIONX"), 0.0)
    }

    @Test
    fun `blind success matches the exact probability for a small puzzle`() {
        val rate = solver("LION", "PAN", "NAP", "OIL", "PLAN").blindSuccessRate("LIONPAN", rollouts = 4000, random = kotlin.random.Random(1))
        assertEquals(0.4, rate, 0.04)
    }

    @Test
    fun `blind success is deterministic for the same letters`() {
        val solver = solver("LION", "PAN", "NAP", "OIL", "PLAN")
        assertEquals(solver.blindSuccessRate("LIONPAN"), solver.blindSuccessRate("NAPLION"), 0.0)
    }

    @Test
    fun `analyze fills the blind success rate`() {
        val solver = solver("LION", "PAN", "NAP", "OIL", "PLAN")
        assertEquals(solver.blindSuccessRate("LIONPAN"), solver.analyze("LIONPAN").blindSuccessRate, 0.0)
    }

    @Test
    fun `hintWord returns a finish word when a finish exists`() {
        val word = solver("LION", "ZEBRA").hintWord("ZLEIBORNA")
        assertTrue(word == "LION" || word == "ZEBRA")
    }

    @Test
    fun `hintWord returns a spellable word when no finish exists`() {
        assertEquals("GEL", solver("GEL", "CAT", "DOG", "EEL", "TOE").hintWord("CADGEL"))
    }

    @Test
    fun `hintWord prefers the longest spellable word when no finish exists`() {
        assertEquals("LACED", solver("ACE", "LACED", "GEL").hintWord("LACEDGX"))
    }

    @Test
    fun `hintWord returns null when nothing is spellable`() {
        assertNull(solver("LION").hintWord("XYZ"))
    }
}
