package com.quietgrid.cli.circuit

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.writePuzzleEntries
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.classifyCircuitDifficulty
import com.quietgrid.engine.circuit.countCircuitSolutions
import com.quietgrid.engine.circuit.solveCircuit
import com.quietgrid.engine.circuit.toBoard
import com.quietgrid.engine.core.Difficulty

data class CircuitTierMove(val id: String, val from: String, val to: String)

data class CircuitUntiered(val id: String, val broken: Boolean, val solverGap: Boolean)

data class CircuitRegradeReport(
    val kept: List<CircuitPuzzleEntry>,
    val moved: List<CircuitTierMove>,
    val untiered: List<CircuitUntiered>,
)

fun regradeCircuitBank(bank: List<CircuitPuzzleEntry>): CircuitRegradeReport {
    val kept = mutableListOf<CircuitPuzzleEntry>()
    val moved = mutableListOf<CircuitTierMove>()
    val untiered = mutableListOf<CircuitUntiered>()
    bank.forEach { puzzle ->
        val board = puzzle.toBoard()
        val shapes = puzzle.solution.toIntArray()
        val result = solveCircuit(board, shapes)
        val tier = classifyCircuitDifficulty(board, result)?.key
        if (tier == null) {
            val broken = countCircuitSolutions(board, shapes) >= 2
            untiered += CircuitUntiered(puzzle.id, broken = broken, solverGap = !broken && !result.solved)
            return@forEach
        }
        if (tier != puzzle.difficulty) moved += CircuitTierMove(puzzle.id, puzzle.difficulty, tier)
        kept += puzzle.copy(difficulty = tier)
    }
    return CircuitRegradeReport(kept, moved, untiered)
}

fun runCircuitRegrade(path: String, write: Boolean) {
    val bank = readPuzzleEntries(path, CircuitPuzzleEntry.serializer())
    val report = regradeCircuitBank(bank)
    Difficulty.entries.forEach { difficulty ->
        val before = bank.count { it.difficulty == difficulty.key }
        val after = report.kept.count { it.difficulty == difficulty.key }
        println("${difficulty.key}: $before -> $after")
    }
    report.moved.groupingBy { "${it.from} -> ${it.to}" }.eachCount().forEach { (move, count) -> println("moved $move: $count") }
    report.moved.forEach { println("  ${it.id}: ${it.from} -> ${it.to}") }
    println(
        "untiered: ${report.untiered.size} (broken ${report.untiered.count { it.broken }}, " +
            "solver gap ${report.untiered.count { it.solverGap }}, " +
            "outside bands ${report.untiered.count { !it.broken && !it.solverGap }})",
    )
    bank.filter { entry -> report.untiered.any { it.id == entry.id } }
        .groupingBy { it.difficulty }.eachCount()
        .forEach { (tier, count) -> println("  untiered from $tier: $count") }
    report.untiered.filter { it.broken }.forEach { println("  broken: ${it.id}") }
    report.untiered.filter { it.solverGap }.forEach { println("  solver gap: ${it.id}") }
    if (!write) return
    writePuzzleEntries(path, report.kept, CircuitPuzzleEntry.serializer())
    println("Wrote ${report.kept.size} puzzles to $path")
}
