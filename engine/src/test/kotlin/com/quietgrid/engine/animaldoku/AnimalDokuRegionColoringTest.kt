package com.quietgrid.engine.animaldoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnimalDokuRegionColoringTest {

    private val sevenRegions = listOf(
        listOf(0, 0, 0, 1, 1, 1, 1),
        listOf(0, 0, 2, 2, 1, 1, 3),
        listOf(4, 4, 2, 2, 2, 3, 3),
        listOf(4, 4, 4, 2, 3, 3, 6),
        listOf(5, 5, 5, 5, 3, 6, 6),
        listOf(5, 5, 5, 6, 6, 6, 6),
        listOf(5, 5, 5, 5, 6, 6, 6),
    )

    private fun touchingPairs(regions: List<List<Int>>): Set<Pair<Int, Int>> {
        val pairs = mutableSetOf<Pair<Int, Int>>()
        for (row in regions.indices) {
            for (col in regions[row].indices) {
                val region = regions[row][col]
                regions.getOrNull(row + 1)?.get(col)?.takeIf { it != region }?.let { pairs += minOf(it, region) to maxOf(it, region) }
                regions[row].getOrNull(col + 1)?.takeIf { it != region }?.let { pairs += minOf(it, region) to maxOf(it, region) }
            }
        }
        return pairs
    }

    private fun forbiddenTouches(regions: List<List<Int>>, colors: List<Int>, forbidden: Set<Pair<Int, Int>>): Int =
        touchingPairs(regions).count { (a, b) ->
            val pair = minOf(colors[a], colors[b]) to maxOf(colors[a], colors[b])
            pair in forbidden
        }

    @Test
    fun `touching regions five and six get colors that are not a forbidden pair`() {
        val forbidden = setOf(5 to 6)
        assert(5 to 6 in touchingPairs(sevenRegions))

        val colors = assignRegionColors(sevenRegions, paletteSize = 9, forbiddenPairs = forbidden)

        assertEquals(7, colors.size)
        assertEquals(7, colors.toSet().size)
        assertEquals(0, forbiddenTouches(sevenRegions, colors, forbidden))
    }

    @Test
    fun `forbidden pairs work in either order`() {
        val colors = assignRegionColors(sevenRegions, paletteSize = 9, forbiddenPairs = setOf(6 to 5))

        assertFalse(setOf(colors[5], colors[6]) == setOf(5, 6))
    }

    @Test
    fun `input without clashes keeps every region on its own index color`() {
        val colors = assignRegionColors(sevenRegions, paletteSize = 9, forbiddenPairs = setOf(0 to 6, 1 to 5))

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), colors)
    }

    @Test
    fun `impossible input falls back to the assignment with the fewest forbidden touches`() {
        val line = listOf(listOf(0, 1, 2))
        val forbidden = setOf(0 to 1, 1 to 2)

        val colors = assignRegionColors(line, paletteSize = 3, forbiddenPairs = forbidden)

        assertEquals(3, colors.toSet().size)
        assertEquals(1, forbiddenTouches(line, colors, forbidden))
    }

    @Test
    fun `more regions than colors falls back to index colors`() {
        val regions = listOf(listOf(0, 1, 2, 3))

        assertEquals(listOf(0, 1, 2, 0), assignRegionColors(regions, paletteSize = 3, forbiddenPairs = setOf(0 to 1)))
    }
}
