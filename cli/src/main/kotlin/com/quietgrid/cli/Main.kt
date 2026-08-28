package com.quietgrid.cli

import com.quietgrid.cli.animaldoku.generateAnimalDokuPuzzleForSolution
import com.quietgrid.cli.animaldoku.generateSolutionPermutation
import com.quietgrid.cli.arrowescape.generateArrowEscapePuzzle
import com.quietgrid.cli.arrowescape.toEntry
import com.quietgrid.cli.sudoku.generateSudokuPuzzle
import com.quietgrid.cli.takuzu.generateTakuzuPuzzle
import com.quietgrid.cli.arrowescape.arrowEscapeSizesForDifficulty
import com.quietgrid.engine.animaldoku.ANIMALDOKU_SIZES_BY_DIFFICULTY
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry

data class GenerateCommand(
    val game: String,
    val difficulty: String,
    val count: Int,
    val outDir: String,
    val locale: String,
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
    require(args.isNotEmpty() && args[0] == "generate") { "Usage: generate --game <id> --difficulty <d> [--count <n>] [--out <dir>] [--locale <l>]" }
    return GenerateCommand(
        game = requireFlag(args, "--game"),
        difficulty = requireFlag(args, "--difficulty"),
        count = optionalFlag(args, "--count", "1").toInt(),
        outDir = optionalFlag(args, "--out", "app/src/main/assets"),
        locale = optionalFlag(args, "--locale", "en"),
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
            val locale = command.locale
            val state = GenerationState("${command.outDir}/.generation-state/wordsearch-$locale.json")
            val maxTotalAttempts = command.count * 30
            val entries = mutableListOf<com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry>()
            var attempts = 0
            while (entries.size < command.count && attempts < maxTotalAttempts) {
                attempts++
                val (rows, cols) = sizes.random()
                val generated = com.quietgrid.cli.wordsearch.generateWordSearchPuzzle(rows, cols, difficulty, preferredLanguages = listOf(locale)) ?: continue
                if (state.hasTried(generated.id)) continue
                state.recordTried(generated.id, "valid")
                entries += generated
            }
            state.save()
            val outFile = "${command.outDir}/wordsearch_puzzles_${difficulty.key}.json"
            appendPuzzleEntries(outFile, entries, com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} wordsearch puzzles ($locale/$difficulty) into $outFile")
        }
        "wordguess" -> {
            val locale = command.locale
            val raw = if (locale == "de") {
                com.quietgrid.cli.wordguess.loadWordGuessFrequencyWords(locale)
            } else {
                com.quietgrid.cli.wordguess.sortWordGuessByRarity(
                    com.quietgrid.cli.wordguess.loadWordGuessHunspellWords(locale),
                    locale,
                )
            }
            val tiers5 = com.quietgrid.cli.wordguess.buildWordGuessTiers(raw, wordLength = 5)
            val tiers6 = com.quietgrid.cli.wordguess.buildWordGuessTiers(raw, wordLength = 6)
            val state = GenerationState("${command.outDir}/.generation-state/wordguess-$locale.json")

            val answerEntries = com.quietgrid.cli.wordguess.generateWordGuessAnswerEntries(locale, difficulty, tiers5, tiers6, command.count, state)
            state.save()
            appendPuzzleEntries(
                "${command.outDir}/wordguess_puzzles.json",
                answerEntries,
                com.quietgrid.engine.wordguess.WordGuessPuzzleEntry.serializer(),
            ) { "${it.locale}:${it.difficulty}:${it.word}" }

            val dictionaryEntries = (tiers5.dictionary + tiers6.dictionary).map {
                com.quietgrid.engine.wordguess.WordGuessDictionaryEntry(locale, it)
            }
            appendPuzzleEntries(
                "${command.outDir}/wordguess_dictionary.json",
                dictionaryEntries,
                com.quietgrid.engine.wordguess.WordGuessDictionaryEntry.serializer(),
            ) { "${it.locale}:${it.word}" }

            println("Generated ${answerEntries.size}/${command.count} wordguess answers ($locale/$difficulty) + ${dictionaryEntries.size} dictionary words into ${command.outDir}")
        }
        "animaldoku" -> {
            val sizes = ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(difficulty)
            val state = GenerationState("${command.outDir}/.generation-state/animaldoku.json")
            val maxTotalAttempts = command.count * 30
            val entries = mutableListOf<AnimalDokuPuzzleEntry>()
            var attempts = 0
            while (entries.size < command.count && attempts < maxTotalAttempts) {
                attempts++
                val size = sizes.random()
                val solution = generateSolutionPermutation(size) ?: continue
                val solutionKey = "$size:$difficulty:${solution.joinToString(",")}"
                if (state.hasTried(solutionKey)) continue
                val candidate = generateAnimalDokuPuzzleForSolution(size, solution, difficulty, idPrefix = "ad$size")
                state.recordTried(solutionKey, if (candidate != null) "valid" else "failed")
                if (candidate != null) entries += candidate
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/animaldoku_puzzles.json", entries, AnimalDokuPuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} animaldoku puzzles at $difficulty into ${command.outDir}/animaldoku_puzzles.json")
        }
        "arrowescape" -> {
            val sizes = arrowEscapeSizesForDifficulty(difficulty)
            val state = GenerationState("${command.outDir}/.generation-state/arrowescape.json")
            val maxTotalAttempts = command.count * 30
            val entries = mutableListOf<ArrowEscapePuzzleEntry>()
            var attempts = 0
            while (entries.size < command.count && attempts < maxTotalAttempts) {
                attempts++
                val size = sizes.random()
                val generated = generateArrowEscapePuzzle(size, size, difficulty) ?: continue
                if (state.hasTried(generated.dedupeKey)) continue
                state.recordTried(generated.dedupeKey, "valid")
                entries += toEntry("ae$size-${entries.size}-${System.nanoTime()}", generated)
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/arrowescape_puzzles.json", entries, ArrowEscapePuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} arrowescape puzzles at $difficulty into ${command.outDir}/arrowescape_puzzles.json")
        }
        else -> error("Unknown or not-yet-wired game '${command.game}'.")
    }
}
