package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchPlacementTest {
    private val allDirections = WordSearchDirection.entries.toList()

    private val largePool = listOf(
        "CAT", "CATS", "DOG", "DOGS", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER", "FROG", "GOAT", "HARE", "MOLE",
        "OWL", "FOX", "ELK", "RAM", "COW", "PIG", "HEN", "ANT", "ANTS", "BEE", "BEES", "BAT", "RAT", "EEL",
        "SEAL", "CRAB", "DUCK", "SWAN", "MOTH", "TOAD", "NEWT", "SLUG", "WASP", "MULE", "PONY", "CALF",
        "GULL", "HAWK", "MOOSE", "TIGER", "ZEBRA", "CAMEL", "SHEEP", "HORSE", "MOUSE", "SNAKE", "WHALE", "SHARK",
        "CUBS", "FAWN", "KIWI", "LYNX", "OTTER", "PANDA", "RAVEN", "ROBIN", "APE", "EMU", "YAK", "GNU", "ASP", "COD",
        "TIT", "DOE", "KID", "LAMB", "FOAL", "BULL", "BOAR", "SOW", "HOG", "MARE", "STAG", "CROW", "DOVE", "WREN",
        "LARK", "KITE", "IBIS", "TERN", "RHEA", "PUMA", "ORCA", "TUNA", "CARP", "PIKE", "SOLE", "HAKE", "CLAM",
        "SNAIL", "EAGLE", "FINCH", "HERON", "STORK", "SKUNK", "LLAMA", "BISON", "HYENA", "KOALA", "LEMUR", "SLOTH",
        "TAPIR", "GECKO", "VIPER", "COBRA", "TROUT", "SQUID", "PRAWN", "HORNET", "BEETLE", "SPIDER", "RABBIT",
        "BADGER", "BEAVER", "FERRET", "GERBIL", "HAMSTER", "DONKEY", "TURKEY", "PIGEON", "PARROT", "TOUCAN",
        "PENGUIN", "GIRAFFE", "LEOPARD", "CHEETAH", "GORILLA", "DOLPHIN", "OSTRICH", "PEACOCK", "PELICAN",
        "ELEPHANT", "KANGAROO", "FLAMINGO", "HEDGEHOG", "SQUIRREL", "TORTOISE", "ANTELOPE", "CROCODILE",
        "BUTTERFLY", "CATERPILLAR", "RHINOCEROS", "HIPPOPOTAMUS", "CHIMPANZEE", "SALAMANDER",
    )

    private fun buildHardGrids(count: Int): List<PlacementResult> = (1..count).mapNotNull {
        buildFullCoverageGrid(
            rows = 12, cols = 12,
            wordPool = largePool,
            reservedCells = setOf(toGridKey(WSCellRef(2, 3)), toGridKey(WSCellRef(7, 9)), toGridKey(WSCellRef(10, 1))),
            allowedDirections = allDirections,
            overlapFrequency = 0.40,
        )
    }

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

    @Test
    fun `buildFullCoverageGrid gives every placed word at least one cell of its own`() {
        val results = buildHardGrids(12)
        assertTrue("expected some grids to tile", results.isNotEmpty())
        results.forEach { result -> assertFalse(hasCoverageViolation(result.placements)) }
    }

    @Test
    fun `buildFullCoverageGrid never lets a placed word appear a second time`() {
        val results = buildHardGrids(12)
        assertTrue("expected some grids to tile", results.isNotEmpty())
        results.forEach { result -> assertFalse(hasDuplicateOccurrence(result.grid, result.placements.map { it.word to it.positions })) }
    }
}
