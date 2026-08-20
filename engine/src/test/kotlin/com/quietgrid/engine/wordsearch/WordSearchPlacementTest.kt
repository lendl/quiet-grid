package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchPlacementTest {
    @Test
    fun `buildFullCoverageGrid tiles a small grid completely with no empty cells`() {
        val result = buildFullCoverageGrid(
            rows = 6, cols = 6,
            wordPool = listOf(
                "CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER", "FROG", "GOAT", "HARE", "MOLE",
                "OWL", "FOX", "ELK", "RAM", "COW", "PIG", "HEN", "ANT", "BEE", "BAT", "RAT", "EEL",
                "SEAL", "CRAB", "DUCK", "SWAN", "MOTH", "TOAD", "NEWT", "SLUG", "WASP", "MULE", "PONY", "CALF",
                "GULL", "HAWK", "MOOSE", "TIGER", "ZEBRA", "CAMEL", "SHEEP", "HORSE", "MOUSE", "SNAKE", "WHALE", "SHARK",
                "ANTS", "BEES", "CUBS", "FAWN", "KIWI", "LYNX", "OTTER", "PANDA", "RAVEN", "ROBIN",
            ),
            reservedCells = emptySet(),
            allowedDirections = listOf(WordSearchDirection.RIGHT, WordSearchDirection.DOWN),
            overlapFrequency = 0.2,
        )
        assertNotNull(result)
        val allCovered = result!!.grid.all { row -> row.all { it.isNotEmpty() && it != "#" } }
        assertTrue(allCovered)
    }
}
