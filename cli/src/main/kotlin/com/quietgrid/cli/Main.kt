package com.quietgrid.cli

import com.quietgrid.cli.sudoku.generateSudokuPuzzle
import com.quietgrid.cli.takuzu.generateTakuzuPuzzle
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry

data class GenerateCommand(
    val game: String,
    val difficulty: String,
    val count: Int,
    val outDir: String,
)

private fun requireFlag(args: Array<String>, flag: String): String {
    val index = args.indexOf(flag)
    require(index != -1 && index + 1 < args.size) { "Missing required flag $flag" }
    return args[index + 1]
}

private fun optionalFlag(args: Array<String>, flag: String, default: String): String {
    val index = args.indexOf(flag)
    return if (index != -1 && index + 1 < args.size) args[index + 1] else default
}

fun parseArgs(args: Array<String>): GenerateCommand {
    require(args.isNotEmpty() && args[0] == "generate") { "Usage: generate --game <id> --difficulty <d> [--count <n>] [--out <dir>]" }
    return GenerateCommand(
        game = requireFlag(args, "--game"),
        difficulty = requireFlag(args, "--difficulty"),
        count = optionalFlag(args, "--count", "1").toInt(),
        outDir = optionalFlag(args, "--out", "app/src/main/assets"),
    )
}

private fun parseDifficulty(value: String): Difficulty =
    Difficulty.entries.find { it.key == value } ?: error("Unknown difficulty '$value'. Expected one of: ${Difficulty.entries.joinToString { it.key }}")

fun main(args: Array<String>) {
    val command = parseArgs(args)
    val difficulty = parseDifficulty(command.difficulty)

    when (command.game) {
        "takuzu" -> {
            val state = GenerationState("${command.outDir}/.generation-state/takuzu.json")
            val entries = (1..command.count).mapNotNull {
                val candidate = generateTakuzuPuzzle(size = 6, targetDifficulty = difficulty, idPrefix = "t6") ?: return@mapNotNull null
                if (state.hasTried(candidate.solution)) return@mapNotNull null
                state.recordTried(candidate.solution, "valid")
                candidate
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/takuzu_puzzles.json", entries, TakuzuPuzzleEntry.serializer()) { it.solution }
            println("Generated ${entries.size}/${command.count} takuzu puzzles at $difficulty into ${command.outDir}/takuzu_puzzles.json")
        }
        "nonogram" -> {
            val seeds = com.quietgrid.cli.nonogram.loadNonogramSeeds(command.outDir)
            require(seeds.isNotEmpty()) { "No nonogram seed puzzles found in ${command.outDir}/nonogram_puzzles.json" }
            val state = GenerationState("${command.outDir}/.generation-state/nonogram.json")
            val entries = (1..command.count).mapNotNull { attempt ->
                val seed = seeds.random()
                val candidate = com.quietgrid.cli.nonogram.generateNonogramVariant(seed.solution, difficulty, variantSeed = System.nanoTime() + attempt, idPrefix = "n${seed.rows}") ?: return@mapNotNull null
                val dedupeKey = candidate.solution.toString()
                if (state.hasTried(dedupeKey)) return@mapNotNull null
                state.recordTried(dedupeKey, "valid")
                candidate
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/nonogram_puzzles.json", entries, com.quietgrid.engine.nonogram.NonogramPuzzleEntry.serializer()) { it.solution.toString() }
            println("Generated ${entries.size}/${command.count} nonogram puzzles at $difficulty into ${command.outDir}/nonogram_puzzles.json")
        }
        "sudoku" -> {
            val state = GenerationState("${command.outDir}/.generation-state/sudoku.json")
            val entries = (1..command.count).mapNotNull {
                val candidate = generateSudokuPuzzle(difficulty, idPrefix = "s9") ?: return@mapNotNull null
                val dedupeKey = candidate.givens.toString()
                if (state.hasTried(dedupeKey)) return@mapNotNull null
                state.recordTried(dedupeKey, "valid")
                candidate
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/sudoku_puzzles.json", entries, SudokuPuzzleEntry.serializer()) { it.givens.toString() }
            println("Generated ${entries.size}/${command.count} sudoku puzzles at $difficulty into ${command.outDir}/sudoku_puzzles.json")
        }
        "wordsearch" -> {
            val sizes = com.quietgrid.engine.wordsearch.wordSearchAllowedSizes(difficulty)
            val state = GenerationState("${command.outDir}/.generation-state/wordsearch.json")
            val entries = (1..command.count).mapNotNull {
                val (rows, cols) = sizes.random()
                val candidate = com.quietgrid.cli.wordsearch.generateWordSearchPuzzle(rows, cols, difficulty) ?: return@mapNotNull null
                if (state.hasTried(candidate.id)) return@mapNotNull null
                state.recordTried(candidate.id, "valid")
                candidate
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/wordsearch_puzzles.json", entries, com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} wordsearch puzzles at $difficulty into ${command.outDir}/wordsearch_puzzles.json")
        }
        else -> error("Unknown or not-yet-wired game '${command.game}'.")
    }
}
