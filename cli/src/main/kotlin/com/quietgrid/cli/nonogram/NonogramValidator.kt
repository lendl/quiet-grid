package com.quietgrid.cli.nonogram

import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramDifficulty
import com.quietgrid.engine.nonogram.buildNonogramClues
import com.quietgrid.engine.nonogram.classifyNonogramDifficulty
import com.quietgrid.engine.nonogram.isDegenerateNonogramPuzzle
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

private const val MIN_FILL_RATIO = 0.35
private const val MAX_FILL_RATIO = 0.75

private sealed interface EntryVerdict {
    data object Ok : EntryVerdict
    data object Unsolvable : EntryVerdict
    data class DegenerateFill(val fillRatio: Double) : EntryVerdict
    data class Mislabeled(val recomputed: String) : EntryVerdict
    data object NoLogicRequired : EntryVerdict
}

private fun verdictFor(entry: NonogramPuzzleEntry): EntryVerdict {
    val fillRatio = entry.solution.sumOf { row -> row.count { it } }.toDouble() / (entry.rows * entry.cols)
    if (fillRatio < MIN_FILL_RATIO || fillRatio > MAX_FILL_RATIO) {
        return EntryVerdict.DegenerateFill(fillRatio)
    }

    val rowClues = entry.solution.map { buildNonogramClues(it) }
    val colClues = (0 until entry.cols).map { c -> buildNonogramClues(entry.solution.map { it[c] }) }
    val metrics = analyzeNonogramDifficulty(rowClues, colClues, entry.solution) ?: return EntryVerdict.Unsolvable

    val recomputed = classifyNonogramDifficulty(entry.rows, entry.cols, metrics)
    if (isDegenerateNonogramPuzzle(entry.rows, entry.cols, recomputed, metrics)) return EntryVerdict.NoLogicRequired
    if (recomputed.key != entry.difficulty) return EntryVerdict.Mislabeled(recomputed.key)

    return EntryVerdict.Ok
}

fun purgeDegenerateNonogramEntries(path: String): Map<String, Int> {
    val file = File(path)
    require(file.exists()) { "No such file: $path" }
    val json = Json { ignoreUnknownKeys = true }
    val listSerializer = ListSerializer(NonogramPuzzleEntry.serializer())
    val entries: List<NonogramPuzzleEntry> = json.decodeFromString(listSerializer, file.readText())

    val (bad, good) = entries.partition { verdictFor(it) != EntryVerdict.Ok }
    val removedByDifficulty = bad.groupingBy { it.difficulty }.eachCount()

    file.writeText(json.encodeToString(listSerializer, good))
    return removedByDifficulty
}

fun validateNonogramBank(path: String) {
    val file = File(path)
    require(file.exists()) { "No such file: $path" }
    val json = Json { ignoreUnknownKeys = true }
    val entries: List<NonogramPuzzleEntry> =
        json.decodeFromString(ListSerializer(NonogramPuzzleEntry.serializer()), file.readText())

    var unsolvable = 0
    var mislabeled = 0
    var degenerateFill = 0
    var noLogicRequired = 0
    val issues = mutableListOf<String>()

    for (entry in entries) {
        when (val verdict = verdictFor(entry)) {
            is EntryVerdict.Ok -> Unit
            is EntryVerdict.Unsolvable -> {
                unsolvable += 1
                issues += "${entry.id}: solver could not reach the stored solution (unsolvable or ambiguous clue set)"
            }
            is EntryVerdict.DegenerateFill -> {
                degenerateFill += 1
                issues += "${entry.id}: fillRatio=${"%.2f".format(verdict.fillRatio)} outside [$MIN_FILL_RATIO,$MAX_FILL_RATIO] (stored=${entry.difficulty})"
            }
            is EntryVerdict.Mislabeled -> {
                mislabeled += 1
                issues += "${entry.id}: stored=${entry.difficulty} recomputed=${verdict.recomputed}"
            }
            is EntryVerdict.NoLogicRequired -> {
                noLogicRequired += 1
                issues += "${entry.id}: no real logic required, or the same trick just repeated across symmetric lines"
            }
        }
    }

    println("Validated ${entries.size} nonogram puzzles from $path")
    println("  unsolvable: $unsolvable")
    println("  mislabeled (stored != recomputed): $mislabeled")
    println("  degenerate fillRatio (outside [$MIN_FILL_RATIO,$MAX_FILL_RATIO]): $degenerateFill")
    println("  no logic required or duplicate-trick junk: $noLogicRequired")
    if (issues.isNotEmpty()) {
        println("First ${minOf(issues.size, 40)} issues:")
        issues.take(40).forEach { println("  $it") }
    }
}
