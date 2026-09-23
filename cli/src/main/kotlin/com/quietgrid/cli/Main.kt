package com.quietgrid.cli

import com.quietgrid.cli.animaldoku.generateAnimalDokuPuzzleForSolution
import com.quietgrid.cli.animaldoku.generateSolutionPermutation
import com.quietgrid.cli.arrowescape.generateArrowEscapePuzzle
import com.quietgrid.cli.arrowescape.toEntry
import com.quietgrid.cli.sudoku.generateSudokuPuzzle
import com.quietgrid.cli.takuzu.generateTakuzuPuzzle
import com.quietgrid.cli.arrowescape.arrowEscapeSizesForDifficulty
import com.quietgrid.cli.starbattle.generateStarBattleK1PuzzleForTier
import com.quietgrid.cli.starbattle.generateStarBattleSolution
import com.quietgrid.cli.starbattle.growStarBattleRegions
import com.quietgrid.cli.starbattle.repairStarBattleRegionsTowardUniqueSolution
import com.quietgrid.cli.starbattle.softenStarBattleTowardGrade
import com.quietgrid.cli.flowfree.generateFlowFreePuzzleForTier
import com.quietgrid.engine.animaldoku.ANIMALDOKU_SIZES_BY_DIFFICULTY
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.scoreArrowEscapePuzzle
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.flowfree.FLOWFREE_PAIR_COUNT_RANGE
import com.quietgrid.engine.flowfree.FLOWFREE_SIZE
import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import com.quietgrid.engine.starbattle.classifyStarBattleK2Grade
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
    if (args.getOrNull(0) == "validate") {
        val game = requireFlag(args, "--game")
        val path = optionalFlag(args, "--path", "app/src/main/assets/${game}_puzzles.json")
        when (game) {
            "nonogram" -> com.quietgrid.cli.nonogram.validateNonogramBank(path)
            else -> error("Validation not implemented for game '$game'")
        }
        return
    }

    if (args.getOrNull(0) == "inspect") {
        val game = requireFlag(args, "--game")
        val path = optionalFlag(args, "--path", "app/src/main/assets/${game}_puzzles.json")
        when (game) {
            "nonogram" -> com.quietgrid.cli.nonogram.inspectNonogramExtremes(path)
            else -> error("Inspect not implemented for game '$game'")
        }
        return
    }

    if (args.getOrNull(0) == "freebie-lines") {
        val game = requireFlag(args, "--game")
        val path = optionalFlag(args, "--path", "app/src/main/assets/${game}_puzzles.json")
        when (game) {
            "nonogram" -> com.quietgrid.cli.nonogram.analyzeFreebieLineRatioDistribution(path)
            else -> error("Not implemented for game '$game'")
        }
        return
    }

    if (args.getOrNull(0) == "purge") {
        val game = requireFlag(args, "--game")
        val path = optionalFlag(args, "--path", "app/src/main/assets/${game}_puzzles.json")
        when (game) {
            "nonogram" -> {
                val removed = com.quietgrid.cli.nonogram.purgeDegenerateNonogramEntries(path)
                println("Purged degenerate nonogram entries from $path: $removed")
            }
            else -> error("Purge not implemented for game '$game'")
        }
        return
    }

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
            val sizes = com.quietgrid.cli.nonogram.nonogramSizesForDifficulty(difficulty)
            val state = GenerationState("${command.outDir}/.generation-state/nonogram.json")
            val maxTotalAttempts = command.count * 50
            val entries = mutableListOf<com.quietgrid.engine.nonogram.NonogramPuzzleEntry>()
            var attempts = 0
            while (entries.size < command.count && attempts < maxTotalAttempts) {
                attempts++
                val (rows, cols) = sizes.random()
                val candidate = com.quietgrid.cli.nonogram.generateRandomNonogramPuzzle(rows, cols, difficulty, idPrefix = "n${rows}x$cols", state = state) ?: continue
                val dedupeKey = candidate.solution.toString()
                if (state.hasTried(dedupeKey)) continue
                state.recordTried(dedupeKey, "valid")
                entries += candidate
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
                val score = scoreArrowEscapePuzzle(generated.pieces, generated.rows, generated.cols)
                println("arrowescape audit: difficulty=$difficulty bottleneckScore=${"%.2f".format(score.bottleneckScore)} needleScore=${"%.2f".format(score.needleScore)} normalizedScore=${"%.2f".format(score.normalizedScore)}")
                entries += toEntry("ae$size-${entries.size}-${System.nanoTime()}", generated)
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/arrowescape_puzzles.json", entries, ArrowEscapePuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} arrowescape puzzles at $difficulty into ${command.outDir}/arrowescape_puzzles.json")
        }
        "starbattle" -> {
            val entries = mutableListOf<StarBattlePuzzleEntry>()
            val k1Source = when (difficulty) {
                Difficulty.EASY -> Difficulty.MEDIUM
                Difficulty.MEDIUM -> Difficulty.HARD
                Difficulty.HARD -> Difficulty.EXPERT
                Difficulty.EXPERT -> null
            }
            val k2TargetCount = when (difficulty) {
                Difficulty.MEDIUM, Difficulty.HARD -> kotlin.math.ceil(command.count * 0.3).toInt()
                Difficulty.EXPERT -> command.count
                Difficulty.EASY -> 0
            }
            if (k2TargetCount > 0) {
                var k2Attempts = 0
                val maxK2Attempts = command.count * 30
                while (entries.size < k2TargetCount && k2Attempts < maxK2Attempts) {
                    k2Attempts++
                    val size = (8..9).random()
                    val solution = generateStarBattleSolution(size, 2) ?: continue
                    val initialRegions = growStarBattleRegions(size, solution) ?: continue
                    val repaired = repairStarBattleRegionsTowardUniqueSolution(size, 2, solution, initialRegions) ?: continue
                    var grade = classifyStarBattleK2Grade(repaired.solveResult)
                    var finalRegions = repaired.regions
                    if (grade != difficulty.key && (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD)) {
                        val softened = softenStarBattleTowardGrade(size, 2, solution, repaired.regions, repaired.solveResult, difficulty.key)
                        grade = classifyStarBattleK2Grade(softened.solveResult)
                        finalRegions = softened.regions
                    }
                    if (grade != difficulty.key) continue
                    entries += StarBattlePuzzleEntry(
                        id = "sbk2$size-${solution.joinToString("_") { it.joinToString(",") }}-${finalRegions.joinToString("") { it.joinToString("") }}",
                        size = size,
                        difficulty = difficulty.key,
                        k = 2,
                        regions = finalRegions,
                        solution = solution,
                    )
                }
            }
            if (k1Source != null && entries.size < command.count) {
                var k1Attempts = 0
                val maxK1Attempts = command.count * 30
                while (entries.size < command.count && k1Attempts < maxK1Attempts) {
                    k1Attempts++
                    val size = ANIMALDOKU_SIZES_BY_DIFFICULTY.getValue(k1Source).random()
                    val entry = generateStarBattleK1PuzzleForTier(size, k1Source, difficulty.key, idPrefix = "sbk1$size") ?: continue
                    entries += entry
                }
            }
            appendPuzzleEntries("${command.outDir}/starbattle_puzzles.json", entries, StarBattlePuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} starbattle puzzles at $difficulty into ${command.outDir}/starbattle_puzzles.json")
        }
        "flowfree" -> {
            val pairCountRange = FLOWFREE_PAIR_COUNT_RANGE.getValue(difficulty)
            val state = GenerationState("${command.outDir}/.generation-state/flowfree.json")
            val maxTotalAttempts = command.count * 3000
            val entries = mutableListOf<FlowFreePuzzleEntry>()
            var attempts = 0
            while (entries.size < command.count && attempts < maxTotalAttempts) {
                attempts++
                val pairCount = pairCountRange.random()
                val entry = generateFlowFreePuzzleForTier(FLOWFREE_SIZE, pairCount, difficulty, idPrefix = "ff7") ?: continue
                if (state.hasTried(entry.id)) continue
                state.recordTried(entry.id, "valid")
                entries += entry
            }
            state.save()
            appendPuzzleEntries("${command.outDir}/flowfree_puzzles.json", entries, FlowFreePuzzleEntry.serializer()) { it.id }
            println("Generated ${entries.size}/${command.count} flowfree puzzles at $difficulty into ${command.outDir}/flowfree_puzzles.json")
        }
        else -> error("Unknown or not-yet-wired game '${command.game}'.")
    }
}
