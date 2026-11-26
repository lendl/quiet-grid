package com.quietgrid.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SkeletonBankTest {
    @Test
    fun `returns the first entry of a bank`() {
        val bank = """
            [{"id":"a","difficulty":"easy","givens":[[null,2],[3,null]],"solution":[[1,2],[3,4]],"extra":1},
             {"id":"b","difficulty":"hard","givens":[[1]],"solution":[[1]]}]
        """.trimIndent()
        val entry = firstSudokuEntry(bank)
        assertEquals("a", entry.id)
        assertEquals(listOf(listOf(null, 2), listOf(3, null)), entry.givens)
        assertEquals(listOf(listOf(1, 2), listOf(3, 4)), entry.solution)
    }
}
