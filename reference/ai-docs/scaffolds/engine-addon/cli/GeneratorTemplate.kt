package com.quietgrid.cli.__game__

import com.quietgrid.engine.__game__.__Game__Difficulty
import com.quietgrid.engine.__game__.__Game__Grid
import com.quietgrid.engine.__game__.classifyDifficulty
import com.quietgrid.engine.__game__.isValidSolution

// __Game__Generator.kt template — offline-only, invoked via:
//   ./gradlew :cli:run --args="generate --game __game__ --difficulty <easy|medium|hard|expert> --count <n> --out app/src/main/assets"
// Writes/merges into app/src/main/assets/__game___puzzles.json. Never ships in the APK, never runs on-device.
// Must guarantee: exactly one solution per generated puzzle, and support being re-run to reclassify/
// regenerate existing entries if difficulty heuristics change later (see [[project_shared_puzzle_engine]]
// for known gaps in this app's dedupe/reclassify path — do not repeat them silently).

data class Generated__Game__Puzzle(
    val id: String,
    val difficulty: __Game__Difficulty,
    val size: Int,
    val solution: String,
)

fun generateOne(size: Int, targetDifficulty: __Game__Difficulty): Generated__Game__Puzzle {
    TODO("__GENERATE_AND_VALIDATE_UNIQUE_SOLUTION__, classify via classifyDifficulty(), retry until it " +
        "matches targetDifficulty")
}

fun getEntryDedupeKey(puzzle: Generated__Game__Puzzle): String {
    TODO("__DEFINE_DEDUPE_KEY__ e.g. normalized solution string")
}
