package com.quietgrid.cli.journey

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.gridTransformById
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class JourneyAssetsTest {
    private val assets = "app/src/main/assets"

    private data class BankRow(val id: String, val difficulty: String, val rating: Int?)

    private fun <T> rows(bank: JourneyBank<T>, difficulty: (T) -> String): List<BankRow> =
        bank.files.flatMap { file -> readPuzzleEntries("$assets/$file", bank.serializer) }
            .map { BankRow(bank.id(it), difficulty(it), bank.rating(it)) }

    @Suppress("UNCHECKED_CAST")
    private fun bankRows(game: String): List<BankRow> = when (game) {
        "animaldoku" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry>) { it.difficulty }
        "arrowescape" -> rows(journeyBank(game) as JourneyBank<ArrowEscapePuzzleEntry>) { it.difficulty }
        "starbattle" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.starbattle.StarBattlePuzzleEntry>) { it.difficulty }
        "sudoku", "minisudoku" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.sudoku.SudokuPuzzleEntry>) { it.difficulty }
        "killersudoku" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry>) { it.difficulty }
        "takuzu" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.takuzu.TakuzuPuzzleEntry>) { it.difficulty }
        "nonogram" -> rows(journeyBank(game) as JourneyBank<com.quietgrid.engine.nonogram.NonogramPuzzleEntry>) { it.difficulty }
        else -> error(game)
    }

    private fun journey(game: String): JourneyFile =
        Json.decodeFromString(JourneyFile.serializer(), File("$assets/journey_$game.json").readText())

    private fun median(values: List<Int>): Double = values.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2.0 }

    @Test
    fun `every journey bank entry has a rating in range`() {
        JOURNEY_GAMES.forEach { game ->
            bankRows(game).forEach { assertTrue("$game ${it.id} rated ${it.rating}", it.rating in 100..1000) }
        }
    }

    @Test
    fun `median rating rises with the difficulty label`() {
        JOURNEY_GAMES.forEach { game ->
            val byTier = bankRows(game).groupBy { it.difficulty }
            val medians = Difficulty.entries.mapNotNull { tier -> byTier[tier.key]?.let { rows -> median(rows.map { it.rating!! }) } }
            medians.zipWithNext().forEach { (lower, higher) -> assertTrue("$game medians $medians", lower < higher) }
        }
    }

    @Test
    fun `every journey file validates against its bank`() {
        JOURNEY_GAMES.forEach { game ->
            val file = journey(game)
            assertEquals(game, file.game)
            validateJourney(file, bankRows(game).map { it.id }.toSet())
            assertEquals((1..file.chapters.size).toList(), file.chapters.map { it.index })
            file.chapters.forEach { chapter ->
                assertTrue("$game chapter ${chapter.index} is empty", chapter.stops.isNotEmpty())
                chapter.stops.forEach { assertTrue("$game ${it.puzzleId}", it.rating in chapter.minRating..chapter.maxRating) }
            }
        }
    }

    @Test
    fun `every stop's difficulty matches its bank entry`() {
        JOURNEY_GAMES.forEach { game ->
            val tiers = bankRows(game).associate { it.id to it.difficulty }
            journey(game).chapters.flatMap { it.stops }.forEach { stop ->
                assertEquals("$game ${stop.puzzleId}", tiers.getValue(stop.puzzleId), stop.difficulty)
            }
        }
    }

    @Test
    fun `halloween puzzles are in no journey and carry no rating`() {
        val halloween = readPuzzleEntries("$assets/arrowescape_halloween_puzzles.json", ArrowEscapePuzzleEntry.serializer())
        val stops = journey("arrowescape").chapters.flatMap { chapter -> chapter.stops.map { it.puzzleId } }.toSet()
        halloween.forEach {
            assertTrue(it.id, it.id !in stops)
            assertEquals(it.id, null, it.rating)
        }
    }

    @Test
    fun `every listed replay transform keeps the stop solvable and within five percent`() {
        val scales = readJourneyScales(DEFAULT_JOURNEY_SCALES)
        JOURNEY_GAMES.forEach { game ->
            val scale = scales.getValue(game).scale
            val check = replayTransformCheck(game, assets, scale)
            journey(game).chapters.flatMap { it.stops }.parallelStream().forEach { stop ->
                stop.replayTransforms.forEach { id ->
                    val rating = check(stop.puzzleId, gridTransformById(id))
                    assertTrue("$game ${stop.puzzleId} transform $id unsolvable", rating != null)
                    assertTrue("$game ${stop.puzzleId} transform $id rated $rating vs ${stop.rating}", abs(rating!! - stop.rating) <= stop.rating * 0.05)
                }
            }
        }
    }

    @Test
    fun `every arrow escape stop replays mirrored`() {
        journey("arrowescape").chapters.flatMap { it.stops }.forEach { assertEquals(it.puzzleId, listOf(1), it.replayTransforms) }
    }

    @Test
    fun `most animal doku stops have a replay transform`() {
        val stops = journey("animaldoku").chapters.flatMap { it.stops }
        val withTransforms = stops.count { it.replayTransforms.isNotEmpty() }
        assertTrue("$withTransforms of ${stops.size}", withTransforms >= stops.size * 0.9)
        stops.forEach { stop -> assertTrue(stop.puzzleId, 0 !in stop.replayTransforms) }
    }
}
