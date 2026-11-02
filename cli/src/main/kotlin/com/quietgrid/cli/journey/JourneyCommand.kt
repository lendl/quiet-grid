package com.quietgrid.cli.journey

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyRatingScale
import kotlinx.serialization.json.Json
import java.io.File

const val DEFAULT_JOURNEY_SCALES = "cli/src/main/resources/journey_rating_scales.json"

data class JourneyArgs(val game: String, val outDir: String, val scalesPath: String)

private val journeyJson = Json { ignoreUnknownKeys = true }

fun parseJourneyArgs(args: Array<String>): JourneyArgs {
    require(args.firstOrNull() == "journey") { "Usage: journey --game <id|all> [--out <dir>] [--scales <path>]" }
    fun flag(name: String): String? = args.indexOf(name).takeIf { it != -1 && it + 1 < args.size }?.let { args[it + 1] }
    return JourneyArgs(
        game = requireNotNull(flag("--game")) { "Missing required flag --game" },
        outDir = flag("--out") ?: "app/src/main/assets",
        scalesPath = flag("--scales") ?: DEFAULT_JOURNEY_SCALES,
    )
}

@Suppress("UNCHECKED_CAST")
fun replayTransformCheck(game: String, outDir: String, scale: JourneyRatingScale): (String, GridTransform) -> Int? {
    val bank = journeyBank(game) as JourneyBank<Any?>
    val byId = bank.files.flatMap { readPuzzleEntries("$outDir/$it", bank.serializer) }.associateBy(bank.id)
    return { id, transform -> bank.replayRating(byId.getValue(id), transform, scale) }
}

fun runJourney(game: String, outDir: String, scalesPath: String) {
    if (game == "all") {
        JOURNEY_GAMES.forEach { runJourney(it, outDir, scalesPath) }
        return
    }
    runJourneyFor(journeyBank(game), outDir, scalesPath)
}

private fun <T> runJourneyFor(bank: JourneyBank<T>, outDir: String, scalesPath: String) {
    val scales = readJourneyScales(scalesPath)
    val stored = scales[bank.game] ?: error("$scalesPath has no entry for ${bank.game}")
    val files = bank.files.associateWith { readPuzzleEntries("$outDir/$it", bank.serializer) }
    val entries = files.values.flatten()
    check(entries.isNotEmpty()) { "No ${bank.game} puzzles found in $outDir" }

    val needsRaw = if (stored.points.isEmpty()) entries else entries.filter { bank.rating(it) == null }
    val raws = needsRaw.parallelStream().map { bank.id(it) to bank.rawScore(it) }.toList().toMap()
    val gameScale = if (stored.points.isEmpty()) {
        stored.copy(points = buildRatingScale(entries.map { raws.getValue(bank.id(it)) }))
            .also { writeJourneyScales(scalesPath, scales + (bank.game to it)) }
    } else {
        stored
    }

    val newIds = entries.filter { bank.rating(it) == null }.map(bank.id).toSet()
    val rated = files.mapValues { (_, list) ->
        list.map { entry -> if (bank.rating(entry) == null) bank.withRating(entry, gameScale.scale.rate(raws.getValue(bank.id(entry)))) else entry }
    }
    rated.forEach { (file, list) -> if (list != files[file]) writePuzzleEntries("$outDir/$file", list, bank.serializer) }

    val puzzles = rated.values.flatten().map { RatedPuzzle(bank.id(it), bank.difficulty(it), bank.rating(it)!!) }
    val journeyFile = File("$outDir/journey_${bank.game}.json")
    val existing = journeyFile.takeIf { it.exists() }?.let { journeyJson.decodeFromString(JourneyFile.serializer(), it.readText()) }
    val journey = existing?.let { mergeJourney(it, puzzles, newIds, gameScale.targetSeconds) }
        ?: buildJourney(bank.game, puzzles, gameScale.targetSeconds)
    validateJourney(journey, puzzles.map { it.id }.toSet())
    val entryById = entries.associateBy(bank.id)
    val replayById = journey.chapters.flatMap { it.stops }.parallelStream().map { stop ->
        val entry = entryById.getValue(stop.puzzleId)
        stop.puzzleId to allowedReplayTransforms(bank.replayCandidates(entry), stop.rating) { bank.replayRating(entry, it, gameScale.scale) }
    }.toList().toMap()
    val withReplays = withReplayTransforms(journey) { replayById.getValue(it.puzzleId) }
    journeyFile.writeText(journeyJson.encodeToString(JourneyFile.serializer(), withReplays))

    val stops = journey.chapters.sumOf { it.stops.size }
    println("Journey ${bank.game}: ${journey.chapters.size} chapters, $stops stops, ${newIds.size} newly rated puzzles into ${journeyFile.path}")
}
