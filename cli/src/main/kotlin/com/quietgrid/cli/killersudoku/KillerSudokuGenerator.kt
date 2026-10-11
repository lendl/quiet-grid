package com.quietgrid.cli.killersudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerDifficultyMetrics
import com.quietgrid.engine.killersudoku.KillerSudokuCage
import com.quietgrid.engine.killersudoku.KillerSolveTrace
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import com.quietgrid.engine.killersudoku.KillerTechnique
import com.quietgrid.engine.killersudoku.applyKillerMove
import com.quietgrid.engine.killersudoku.createKillerSolverState
import com.quietgrid.engine.killersudoku.KILLER_MIN_CLASSIC_TAKEOVER
import com.quietgrid.engine.killersudoku.KILLER_MIN_SCORE
import com.quietgrid.engine.killersudoku.classifyKillerDifficulty
import com.quietgrid.engine.killersudoku.collectKillerMetrics
import com.quietgrid.engine.killersudoku.killerTechniquesUpTo
import com.quietgrid.engine.killersudoku.measureClassicTakeoverFill
import com.quietgrid.engine.killersudoku.traceKillerSolve
import com.quietgrid.engine.sudoku.cellBoxIndexes
import com.quietgrid.engine.sudoku.cellColIndexes
import com.quietgrid.engine.sudoku.cellRowIndexes
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import kotlin.random.Random

private val CAGE_SIZE_WEIGHTS: Map<Difficulty, Map<Int, Int>> = mapOf(
    Difficulty.EASY to mapOf(2 to 5, 3 to 4, 4 to 1),
    Difficulty.MEDIUM to mapOf(2 to 3, 3 to 4, 4 to 3, 5 to 1),
    Difficulty.HARD to mapOf(2 to 2, 3 to 4, 4 to 4, 5 to 2),
    Difficulty.EXPERT to mapOf(2 to 2, 3 to 3, 4 to 4, 5 to 3, 6 to 1),
)

private val MAX_SINGLETONS: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 2, Difficulty.MEDIUM to 1, Difficulty.HARD to 1, Difficulty.EXPERT to 0,
)

private const val MAX_CAGE_SIZE = 6
private const val REPAIR_ATTEMPTS = 40

fun killerNeighbors(cell: Int): List<Int> {
    val row = cell / 9
    val col = cell % 9
    return listOfNotNull(
        if (row > 0) cell - 9 else null,
        if (row < 8) cell + 9 else null,
        if (col > 0) cell - 1 else null,
        if (col < 8) cell + 1 else null,
    )
}

fun randomKillerSolution(random: Random): IntArray {
    val board = IntArray(81)
    fun fill(cell: Int): Boolean {
        if (cell == 81) return true
        for (digit in (1..9).shuffled(random)) {
            val clash = (0 until 81).any { other ->
                board[other] == digit &&
                    (cellRowIndexes[other] == cellRowIndexes[cell] || cellColIndexes[other] == cellColIndexes[cell] || cellBoxIndexes[other] == cellBoxIndexes[cell])
            }
            if (clash) continue
            board[cell] = digit
            if (fill(cell + 1)) return true
            board[cell] = 0
        }
        return false
    }
    check(fill(0))
    return board
}

private fun pickSize(weights: Map<Int, Int>, random: Random): Int {
    var roll = random.nextInt(weights.values.sum())
    for ((size, weight) in weights) {
        if (roll < weight) return size
        roll -= weight
    }
    return weights.keys.first()
}

fun growKillerCages(solution: IntArray, difficulty: Difficulty, random: Random): MutableList<MutableList<Int>> {
    val owner = IntArray(81) { -1 }
    val cages = mutableListOf<MutableList<Int>>()
    val weights = CAGE_SIZE_WEIGHTS.getValue(difficulty)

    while (true) {
        val open = (0 until 81).filter { owner[it] == -1 }
        if (open.isEmpty()) break
        val seed = open.minWith(compareBy<Int> { cell -> killerNeighbors(cell).count { owner[it] == -1 } }.thenBy { random.nextInt() })
        val target = pickSize(weights, random)
        val cage = mutableListOf(seed)
        owner[seed] = cages.size
        while (cage.size < target) {
            val digits = cage.map { solution[it] }.toSet()
            val frontier = cage.flatMap { killerNeighbors(it) }.distinct().filter { owner[it] == -1 && solution[it] !in digits }
            if (frontier.isEmpty()) break
            val next = frontier.minWith(compareBy<Int> { cell -> killerNeighbors(cell).count { owner[it] == -1 } }.thenBy { random.nextInt() })
            cage += next
            owner[next] = cages.size
        }
        cages += cage
    }

    for (cage in cages.filter { it.size == 1 }) {
        val cell = cage[0]
        val host = killerNeighbors(cell)
            .map { neighbor -> cages.first { neighbor in it } }
            .filter { it !== cage && it.size < MAX_CAGE_SIZE && it.none { member -> solution[member] == solution[cell] } }
            .minByOrNull { it.size } ?: continue
        host += cell
        cage.clear()
    }
    cages.removeAll { it.isEmpty() }
    return cages
}

private fun isConnected(cells: List<Int>): Boolean {
    if (cells.isEmpty()) return false
    val set = cells.toSet()
    val seen = mutableSetOf(cells[0])
    val queue = ArrayDeque(listOf(cells[0]))
    while (queue.isNotEmpty()) {
        killerNeighbors(queue.removeFirst()).filter { it in set && seen.add(it) }.forEach { queue.addLast(it) }
    }
    return seen.size == set.size
}

private fun splitCage(cage: MutableList<Int>, minPart: Int, random: Random): List<MutableList<Int>>? {
    if (cage.size < minPart * 2) return null
    repeat(12) {
        val first = mutableListOf(cage.random(random))
        val want = minPart + random.nextInt(cage.size - minPart * 2 + 1)
        while (first.size < want) {
            val frontier = first.flatMap { killerNeighbors(it) }.filter { it in cage && it !in first }
            if (frontier.isEmpty()) break
            first += frontier.random(random)
        }
        val second = cage.filter { it !in first }.toMutableList()
        if (first.size >= minPart && second.size >= minPart && isConnected(second)) return listOf(first, second)
    }
    return null
}

private fun moveCell(cages: MutableList<MutableList<Int>>, cell: Int, solution: IntArray, random: Random): Boolean {
    val from = cages.first { cell in it }
    if (from.size < 2 || !isConnected(from.filter { it != cell })) return false
    val target = killerNeighbors(cell)
        .map { neighbor -> cages.first { neighbor in it } }
        .filter { it !== from && it.size < MAX_CAGE_SIZE && it.none { member -> solution[member] == solution[cell] } }
        .shuffled(random)
        .firstOrNull() ?: return false
    from.remove(cell)
    target += cell
    return true
}

fun toKillerLayout(cages: List<List<Int>>, solution: IntArray): KillerCageLayout =
    KillerCageLayout(cages.map { cells -> KillerSudokuCage(cells.sumOf { solution[it] }, cells.sorted()) }.sortedBy { it.cells.first() })

private fun stuckCells(layout: KillerCageLayout, trace: KillerSolveTrace): List<Int> {
    val state = createKillerSolverState(layout)
    trace.moves.forEach { applyKillerMove(state, it) }
    return (0 until 81).filter { state.grid.board[it] == 0 }
}

fun repairKillerLayout(
    cages: MutableList<MutableList<Int>>,
    solution: IntArray,
    allowed: Set<KillerTechnique>,
    maxSingletons: Int,
    random: Random,
): Pair<KillerCageLayout, KillerSolveTrace>? {
    repeat(REPAIR_ATTEMPTS) {
        val layout = toKillerLayout(cages, solution)
        val trace = traceKillerSolve(layout, allowed = allowed)
        if (trace.solved) return (layout to trace).takeIf { cages.count { it.size == 1 } <= maxSingletons }
        val open = stuckCells(layout, trace).shuffled(random)
        if (random.nextInt(3) > 0 && open.take(4).any { moveCell(cages, it, solution, random) }) return@repeat
        val minPart = if (cages.count { it.size == 1 } < maxSingletons) 1 else 2
        val cage = open.map { cell -> cages.first { cell in it } }.firstOrNull { it.size >= minPart * 2 } ?: return@repeat
        val parts = splitCage(cage, minPart, random) ?: return@repeat
        cages.remove(cage)
        cages.addAll(parts)
    }
    return null
}

data class KillerGenerationResult(val entry: KillerSudokuPuzzleEntry, val metrics: KillerDifficultyMetrics, val classicTakeover: Int)

fun generateKillerSudokuPuzzle(targetDifficulty: Difficulty, random: Random = Random.Default): KillerGenerationResult? {
    val solution = randomKillerSolution(random)
    val cages = growKillerCages(solution, targetDifficulty, random)
    val allowed = killerTechniquesUpTo(if (targetDifficulty == Difficulty.EXPERT) Difficulty.HARD else targetDifficulty)
    val (layout, trace) = repairKillerLayout(cages, solution, allowed, MAX_SINGLETONS.getValue(targetDifficulty), random) ?: return null
    val metrics = collectKillerMetrics(trace)
    if (classifyKillerDifficulty(metrics) != targetDifficulty) return null
    if (metrics.score < KILLER_MIN_SCORE.getValue(targetDifficulty)) return null
    val takeover = measureClassicTakeoverFill(layout, trace, allowed)
    if (takeover < KILLER_MIN_CLASSIC_TAKEOVER.getValue(targetDifficulty)) return null
    val solutionKey = solution.joinToString("")
    val cageKey = layout.cages.joinToString("|") { cage -> cage.cells.joinToString(",") }
    val entry = KillerSudokuPuzzleEntry(
        id = "ks-${targetDifficulty.key}-${(solutionKey + cageKey).hashCode().toUInt().toString(36)}",
        difficulty = targetDifficulty.key,
        cages = layout.cages,
        solution = (0 until 9).map { row -> (0 until 9).map { col -> solution[row * 9 + col] } },
    )
    return KillerGenerationResult(entry, metrics, takeover)
}

fun generateKillerSudokuBatch(
    targetDifficulty: Difficulty,
    count: Int,
    threads: Int,
    maxAttempts: Int,
    isNew: (KillerSudokuPuzzleEntry) -> Boolean = { true },
): List<KillerGenerationResult> {
    val accepted = mutableListOf<KillerGenerationResult>()
    val acceptedIds = mutableSetOf<String>()
    val executor = Executors.newFixedThreadPool(threads)
    val completion = ExecutorCompletionService<KillerGenerationResult?>(executor)
    var submitted = 0
    var pending = 0

    fun submitNext() {
        val seed = System.nanoTime() + submitted
        completion.submit(Callable { generateKillerSudokuPuzzle(targetDifficulty, Random(seed)) })
        submitted += 1
        pending += 1
    }

    try {
        while (pending < threads && submitted < maxAttempts) submitNext()
        while (pending > 0) {
            val result = completion.take().get()
            pending -= 1
            if (result != null && accepted.size < count && result.entry.id !in acceptedIds && isNew(result.entry)) {
                accepted += result
                acceptedIds += result.entry.id
            }
            if (accepted.size < count && submitted < maxAttempts) submitNext()
        }
    } finally {
        executor.shutdownNow()
    }
    return accepted
}
