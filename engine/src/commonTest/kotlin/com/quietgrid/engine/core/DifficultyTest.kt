package com.quietgrid.engine.core

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class DifficultyTest {
    @Test
    fun `key is lowercase name`() {
        assertEquals("easy", Difficulty.EASY.key)
        assertEquals("medium", Difficulty.MEDIUM.key)
        assertEquals("hard", Difficulty.HARD.key)
        assertEquals("expert", Difficulty.EXPERT.key)
    }

    @Test
    fun `natural ordering follows easy to expert`() {
        assertTrue(Difficulty.EASY < Difficulty.MEDIUM)
        assertTrue(Difficulty.MEDIUM < Difficulty.HARD)
        assertTrue(Difficulty.HARD < Difficulty.EXPERT)
    }
}
