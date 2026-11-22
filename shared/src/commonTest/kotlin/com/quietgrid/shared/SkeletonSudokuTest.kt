package com.quietgrid.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SkeletonSudokuTest {
    @Test
    fun `solves the skeleton puzzle to its known solution`() {
        val expected = listOf(
            "534678912", "672195348", "198342567",
            "859761423", "426853791", "713924856",
            "961537284", "287419635", "345286179",
        ).map { row -> row.map { it.digitToInt() } }
        assertEquals(expected, solvedSkeletonSudoku())
    }

    @Test
    fun `keeps every given of the skeleton puzzle`() {
        val solved = solvedSkeletonSudoku().flatten()
        SKELETON_PUZZLE.forEachIndexed { index, char ->
            if (char != '0') assertEquals(char.digitToInt(), solved[index], "cell $index")
        }
    }
}
