package com.quietgrid.cli.nonogram

import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramDifficulty
import com.quietgrid.engine.nonogram.buildNonogramClues
import com.quietgrid.engine.nonogram.computeNonogramScore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

private fun renderSolution(entry: NonogramPuzzleEntry): String =
    entry.solution.joinToString("\n") { row -> row.joinToString("") { if (it) "#" else "." } }

fun inspectNonogramExtremes(path: String) {
    val file = File(path)
    require(file.exists()) { "No such file: $path" }
    val json = Json { ignoreUnknownKeys = true }
    val entries: List<NonogramPuzzleEntry> =
        json.decodeFromString(ListSerializer(NonogramPuzzleEntry.serializer()), file.readText())

    val byDifficulty = entries.groupBy { it.difficulty }
    for (difficulty in listOf("easy", "medium", "hard", "expert")) {
        val bucket = byDifficulty[difficulty] ?: continue
        val scored = bucket.mapNotNull { entry ->
            val rowClues = entry.solution.map { buildNonogramClues(it) }
            val colClues = (0 until entry.cols).map { c -> buildNonogramClues(entry.solution.map { it[c] }) }
            val metrics = analyzeNonogramDifficulty(rowClues, colClues, entry.solution) ?: return@mapNotNull null
            Triple(entry, metrics, computeNonogramScore(metrics))
        }

        val easiest = scored.minByOrNull { it.third }
        val mostFreebie = scored.maxByOrNull { it.second.freebieFillRatio }

        println("=== $difficulty (${bucket.size} puzzles) ===")
        if (easiest != null) {
            val (entry, metrics, score) = easiest
            println("-- lowest score: ${entry.id} (${entry.rows}x${entry.cols}) score=$score steps=${metrics.steps} freebieFillRatio=${"%.2f".format(metrics.freebieFillRatio)} hardestTier=${metrics.hardestLineTier}")
            println(renderSolution(entry))
        }
        if (mostFreebie != null) {
            val (entry, metrics, score) = mostFreebie
            println("-- highest freebieFillRatio: ${entry.id} (${entry.rows}x${entry.cols}) score=$score steps=${metrics.steps} freebieFillRatio=${"%.2f".format(metrics.freebieFillRatio)} hardestTier=${metrics.hardestLineTier}")
            println(renderSolution(entry))
        }
        println()
    }
}
