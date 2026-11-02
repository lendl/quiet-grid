package com.quietgrid.cli.journey

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.animalDokuRawScore
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.arrowEscapeRawScore
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.journey.JOURNEY_TIER_SPAN
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class JourneyRawScoreTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val animalDoku = json.decodeFromString(ListSerializer(AnimalDokuPuzzleEntry.serializer()), File("app/src/main/assets/animaldoku_puzzles.json").readText())
    private val arrowEscape = Difficulty.entries.flatMap { tier ->
        json.decodeFromString(ListSerializer(ArrowEscapePuzzleEntry.serializer()), File("app/src/main/assets/arrowescape_puzzles_${tier.key}.json").readText()).take(5)
    }

    private fun <T> samplePerTier(entries: List<T>, difficulty: (T) -> String, count: Int): List<T> =
        Difficulty.entries.flatMap { tier -> entries.filter { difficulty(it) == tier.key }.take(count) }

    @Test
    fun `raw scores are deterministic`() {
        samplePerTier(animalDoku, { it.difficulty }, 3).forEach { assertEquals(it.id, animalDokuRawScore(it), animalDokuRawScore(it)) }
        samplePerTier(arrowEscape, { it.difficulty }, 3).forEach { assertEquals(it.id, arrowEscapeRawScore(it), arrowEscapeRawScore(it)) }
    }

    @Test
    fun `raw scores sit inside their tier slot`() {
        samplePerTier(animalDoku, { it.difficulty }, 5).forEach {
            assertEquals(it.id, Difficulty.entries.first { tier -> tier.key == it.difficulty }.ordinal.toLong(), animalDokuRawScore(it) / JOURNEY_TIER_SPAN)
        }
        samplePerTier(arrowEscape, { it.difficulty }, 5).forEach {
            assertEquals(it.id, Difficulty.entries.first { tier -> tier.key == it.difficulty }.ordinal.toLong(), arrowEscapeRawScore(it) / JOURNEY_TIER_SPAN)
        }
    }

    @Test
    fun `unsolved animaldoku rates in its tier without throwing`() {
        val entry = AnimalDokuPuzzleEntry(
            id = "broken",
            size = 5,
            difficulty = "hard",
            regions = List(5) { List(5) { 0 } },
            solution = listOf(0, 2, 4, 1, 3),
        )
        assertEquals(2L, animalDokuRawScore(entry) / JOURNEY_TIER_SPAN)
    }
}
