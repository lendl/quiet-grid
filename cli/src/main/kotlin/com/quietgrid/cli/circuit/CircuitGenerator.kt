package com.quietgrid.cli.circuit

import com.quietgrid.engine.circuit.CIRCUIT_DIRECTIONS
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CircuitBoard
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.CircuitSolveResult
import com.quietgrid.engine.circuit.CircuitSolverState
import com.quietgrid.engine.circuit.CIRCUIT_TIERS
import com.quietgrid.engine.circuit.CircuitTier
import com.quietgrid.engine.circuit.analyzeCircuitSolveResult
import com.quietgrid.engine.circuit.circuitTierDistance
import com.quietgrid.engine.circuit.classifyCircuitDifficulty
import com.quietgrid.engine.circuit.circuitArmCount
import com.quietgrid.engine.circuit.circuitOrientations
import com.quietgrid.engine.circuit.oppositeCircuitDirection
import com.quietgrid.engine.circuit.solveCircuitFrom
import com.quietgrid.engine.core.Difficulty
import java.security.MessageDigest
import kotlin.random.Random

const val CIRCUIT_MAX_ARMS = 3

fun circuitSourceCell(rows: Int, cols: Int): Int = (rows / 2) * cols + cols / 2

fun randomCircuitTree(board: CircuitBoard, random: Random): IntArray? {
    val masks = IntArray(board.size)
    val inTree = BooleanArray(board.size)
    val start = random.nextInt(board.size)
    inTree[start] = true
    var added = 1
    val frontier = ArrayList<Pair<Int, Int>>()
    fun addFrontier(cell: Int) {
        for (direction in CIRCUIT_DIRECTIONS) {
            val other = board.neighbor(cell, direction)
            if (other >= 0 && !inTree[other]) frontier += cell to direction
        }
    }
    addFrontier(start)
    while (added < board.size) {
        frontier.removeAll { (cell, direction) -> inTree[board.neighbor(cell, direction)] || circuitArmCount(masks[cell]) >= CIRCUIT_MAX_ARMS }
        if (frontier.isEmpty()) return null
        val (cell, direction) = frontier[random.nextInt(frontier.size)]
        val other = board.neighbor(cell, direction)
        masks[cell] = masks[cell] or direction
        masks[other] = masks[other] or oppositeCircuitDirection(direction)
        inTree[other] = true
        added++
        addFrontier(other)
    }
    return masks
}

fun mutateCircuitTree(board: CircuitBoard, masks: IntArray, random: Random): IntArray? {
    val links = ArrayList<Pair<Int, Int>>()
    for (cell in 0 until board.size) {
        for (direction in intArrayOf(CIRCUIT_RIGHT, CIRCUIT_DOWN)) if (masks[cell] and direction != 0) links += cell to direction
    }
    val (cell, direction) = links[random.nextInt(links.size)]
    val next = masks.copyOf()
    val other = board.neighbor(cell, direction)
    next[cell] = next[cell] and direction.inv()
    next[other] = next[other] and oppositeCircuitDirection(direction).inv()
    val side = BooleanArray(board.size)
    val queue = ArrayDeque(listOf(cell))
    side[cell] = true
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        for (d in CIRCUIT_DIRECTIONS) {
            if (next[current] and d == 0) continue
            val n = board.neighbor(current, d)
            if (!side[n]) {
                side[n] = true
                queue.add(n)
            }
        }
    }
    val candidates = ArrayList<Pair<Int, Int>>()
    for (a in 0 until board.size) {
        if (!side[a] || circuitArmCount(next[a]) >= CIRCUIT_MAX_ARMS) continue
        for (d in CIRCUIT_DIRECTIONS) {
            val b = board.neighbor(a, d)
            if (b < 0 || side[b] || circuitArmCount(next[b]) >= CIRCUIT_MAX_ARMS) continue
            if (a == cell && b == other) continue
            candidates += a to d
        }
    }
    if (candidates.isEmpty()) return null
    val (a, d) = candidates[random.nextInt(candidates.size)]
    next[a] = next[a] or d
    next[board.neighbor(a, d)] = next[board.neighbor(a, d)] or oppositeCircuitDirection(d)
    if (next.any { it == 0 }) return null
    return next
}

fun circuitUndecidedAfterLogic(board: CircuitBoard, masks: IntArray): Pair<Int, CircuitSolveResult> {
    val state = CircuitSolverState.fromShapes(board, masks)
    val result = solveCircuitFrom(state)
    return (0 until board.size).count { !state.isDecided(it) } to result
}

fun scrambleCircuit(solution: IntArray, random: Random): IntArray = IntArray(solution.size) { cell ->
    val others = circuitOrientations(solution[cell]).filter { it != solution[cell] }
    if (others.isEmpty()) solution[cell] else others[random.nextInt(others.size)]
}

fun climbCircuitTree(board: CircuitBoard, tier: CircuitTier, random: Random, maxSteps: Int = 600): Pair<IntArray, CircuitSolveResult>? {
    var masks = randomCircuitTree(board, random) ?: return null
    fun score(candidate: IntArray): Pair<Int, CircuitSolveResult> {
        val (undecided, result) = circuitUndecidedAfterLogic(board, candidate)
        val distance = if (undecided == 0 && result.solved) circuitTierDistance(tier, analyzeCircuitSolveResult(result)) else 0
        return undecided * 1000 + distance to result
    }
    var (current, result) = score(masks)
    var steps = 0
    while (current > 0 && steps < maxSteps) {
        steps++
        val candidate = mutateCircuitTree(board, masks, random) ?: continue
        val (candidateScore, candidateResult) = score(candidate)
        if (candidateScore <= current) {
            masks = candidate
            current = candidateScore
            result = candidateResult
        }
    }
    return if (current == 0 && result.solved) masks to result else null
}

fun generateCircuitPuzzle(difficulty: Difficulty, random: Random = Random.Default, maxAttempts: Int = 50): CircuitPuzzleEntry? {
    val tier = CIRCUIT_TIERS.getValue(difficulty)
    val board = CircuitBoard(tier.rows, tier.cols, circuitSourceCell(tier.rows, tier.cols), tier.wrap)
    repeat(maxAttempts) {
        val (solution, result) = climbCircuitTree(board, tier, random) ?: return@repeat
        if (classifyCircuitDifficulty(board, result) != difficulty) return@repeat
        return CircuitPuzzleEntry(
            id = "c${tier.rows}x${tier.cols}${if (tier.wrap) "w" else ""}-${shortDigest(solution.joinToString(","))}",
            rows = tier.rows,
            cols = tier.cols,
            difficulty = difficulty.key,
            source = board.source,
            wrap = tier.wrap,
            solution = solution.toList(),
            start = scrambleCircuit(solution, random).toList(),
        )
    }
    return null
}

private fun shortDigest(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).take(6).joinToString("") { "%02x".format(it) }
