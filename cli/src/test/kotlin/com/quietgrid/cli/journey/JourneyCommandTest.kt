package com.quietgrid.cli.journey

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.journey.JourneyFile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class JourneyCommandTest {
    private val dir = Files.createTempDirectory("journey").toFile().apply { deleteOnExit() }
    private val bankPath = "${dir.path}/animaldoku_puzzles.json"
    private val scalesPath = "${dir.path}/scales.json"
    private val journeyPath = "${dir.path}/journey_animaldoku.json"

    private fun readBank() = readPuzzleEntries(bankPath, AnimalDokuPuzzleEntry.serializer())

    private fun readJourney() = Json.decodeFromString(JourneyFile.serializer(), File(journeyPath).readText())

    private fun seed() {
        val source = readPuzzleEntries("app/src/main/assets/animaldoku_puzzles.json", AnimalDokuPuzzleEntry.serializer())
        val sample = Difficulty.entries.flatMap { tier -> source.filter { it.difficulty == tier.key }.take(50) }.map { it.copy(rating = null) }
        writePuzzleEntries(bankPath, sample, AnimalDokuPuzzleEntry.serializer())
        writeJourneyScales(scalesPath, mapOf("animaldoku" to JourneyGameScale(JourneyTargetSeconds(60, 900))))
    }

    @Test
    fun `first run rates the bank and writes a valid journey`() {
        seed()
        runJourney("animaldoku", dir.path, scalesPath)
        val bank = readBank()
        bank.forEach { assertTrue(it.id, it.rating in 100..1000) }
        assertTrue(readJourneyScales(scalesPath).getValue("animaldoku").points.isNotEmpty())
        val journey = readJourney()
        validateJourney(journey, bank.map { it.id }.toSet())
        assertTrue(journey.chapters.size in 1..4)
        assertEquals((1..journey.chapters.size).toList(), journey.chapters.map { it.index })
    }

    @Test
    fun `second run changes nothing`() {
        seed()
        runJourney("animaldoku", dir.path, scalesPath)
        val before = listOf(bankPath, scalesPath, journeyPath).map { File(it).readBytes().toList() }
        runJourney("animaldoku", dir.path, scalesPath)
        assertEquals(before, listOf(bankPath, scalesPath, journeyPath).map { File(it).readBytes().toList() })
    }

    @Test
    fun `added puzzle gets a rating and leaves existing stops alone`() {
        seed()
        runJourney("animaldoku", dir.path, scalesPath)
        val stopsBefore = readJourney().chapters.map { chapter -> chapter.stops.map { it.puzzleId } }
        val stopIds = stopsBefore.flatten().toSet()
        val bank = readBank()
        val target = bank.first { it.id !in stopIds }
        writePuzzleEntries(bankPath, bank.map { if (it.id == target.id) it.copy(rating = null) else it }, AnimalDokuPuzzleEntry.serializer())
        runJourney("animaldoku", dir.path, scalesPath)
        assertNotNull(readBank().first { it.id == target.id }.rating)
        val stopsAfter = readJourney().chapters.map { chapter -> chapter.stops.map { it.puzzleId } }
        stopsBefore.forEachIndexed { index, ids -> assertEquals(ids, stopsAfter[index].filter { it in ids.toSet() }) }
    }

    @Test
    fun `arrowescape bank never reads the halloween file`() {
        assertTrue(journeyBank("arrowescape").files.none { "halloween" in it })
    }
}
