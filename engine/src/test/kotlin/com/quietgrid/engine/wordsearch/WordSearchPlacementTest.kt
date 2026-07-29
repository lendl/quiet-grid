package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchPlacementTest {
    @Test
    fun `buildFullCoverageGrid tiles a small grid completely with no empty cells`() {
        // NOTE: widened from the original 12-word brief fixture, which was
        // measured (via a faithful line-for-line reimplementation of the TS
        // reference, run standalone outside Gradle) to fail to tile this 6x6
        // grid with only right/down directions ~100% of the time -- a real
        // fixture infeasibility (too little letter overlap among 12 words for
        // full coverage with just 2 directions), not a bug in the port. This
        // 58-word pool was measured at ~99% success over 300 trials of the
        // same reference algorithm, per the brief's own guidance to widen the
        // word pool rather than weaken the "fully tiled" assertion.
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
