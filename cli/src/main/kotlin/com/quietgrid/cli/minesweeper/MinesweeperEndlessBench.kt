package com.quietgrid.cli.minesweeper

import com.quietgrid.engine.minesweeper.endless.ENDLESS_OPENING_CAP
import com.quietgrid.engine.minesweeper.endless.EndlessBuildStats
import com.quietgrid.engine.minesweeper.endless.simulateEndlessRun
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.random.Random

private const val LEVEL_BUCKET = 10

data class EndlessBenchReport(
    val runs: Int,
    val sectionsCleared: Int,
    val stuckRuns: Int,
    val mineHitRuns: Int,
    val lastPathHits: Int,
    val strandedSections: Int = 0,
    val passRateByLevel: Map<Int, Double>,
    val settleP50Millis: Double,
    val settleP99Millis: Double,
    val candidatesByLevel: Map<Int, Int> = emptyMap(),
    val passesByLevel: Map<Int, Int> = emptyMap(),
    val lastPathByLevel: Map<Int, Int> = emptyMap(),
    val largestOpening: Int = 0,
    val runsOverOpeningCap: Int = 0,
    val maxBuildsPerSettle: Int = 0,
)

fun runMinesweeperEndlessBench(sections: Int, runLength: Int, threads: Int, seed: Long): EndlessBenchReport {
    val runs = ceil(sections.toDouble() / runLength).toInt()
    val pool = Executors.newFixedThreadPool(threads)
    val results = try {
        (0 until runs)
            .map { index -> pool.submit(Callable { simulateEndlessRun(runLength, Random(seed + index)) }) }
            .map { it.get() }
    } finally {
        pool.shutdown()
    }
    val stats = results.fold(EndlessBuildStats()) { total, result -> total + result.stats }
    val passes = stats.candidatesByLevel.keys.associateWith { level ->
        (stats.buildsByLevel[level] ?: 0) - (stats.lastPathByLevel[level] ?: 0)
    }
    val settleTimes = results.flatMap { it.settleMillis }.sorted()
    return EndlessBenchReport(
        runs = runs,
        sectionsCleared = results.sumOf { it.sectionsCleared },
        stuckRuns = results.count { it.stuck },
        mineHitRuns = results.count { it.hitMine },
        lastPathHits = stats.lastPathHits,
        strandedSections = results.sumOf { it.strandedSections },
        passRateByLevel = stats.candidatesByLevel.filterValues { it > 0 }
            .mapValues { (level, candidates) -> (passes[level] ?: 0).toDouble() / candidates },
        settleP50Millis = percentile(settleTimes, 0.50),
        settleP99Millis = percentile(settleTimes, 0.99),
        candidatesByLevel = stats.candidatesByLevel,
        passesByLevel = passes,
        lastPathByLevel = stats.lastPathByLevel,
        largestOpening = results.maxOfOrNull { it.largestOpening } ?: 0,
        runsOverOpeningCap = results.count { it.largestOpening > ENDLESS_OPENING_CAP },
        maxBuildsPerSettle = results.flatMap { it.buildsPerSettle }.maxOrNull() ?: 0,
    )
}

private fun percentile(sorted: List<Double>, fraction: Double): Double =
    if (sorted.isEmpty()) 0.0 else sorted[((sorted.size - 1) * fraction).toInt()]

fun formatEndlessBenchReport(report: EndlessBenchReport): String = buildString {
    appendLine("runs: ${report.runs}")
    appendLine("sections cleared: ${report.sectionsCleared}")
    appendLine("stuck runs: ${report.stuckRuns}")
    appendLine("mine-hit runs: ${report.mineHitRuns}")
    appendLine("last-path hits: ${report.lastPathHits}")
    appendLine("stranded sections: ${report.strandedSections}")
    appendLine("largest opening: ${report.largestOpening}")
    appendLine("runs over opening cap: ${report.runsOverOpeningCap}")
    appendLine("max builds per settle: ${report.maxBuildsPerSettle}")
    appendLine("settle per tap p50: ${"%.2f".format(report.settleP50Millis)} ms, p99: ${"%.2f".format(report.settleP99Millis)} ms")
    val buckets = report.passRateByLevel.keys.groupBy { (it - 1) / LEVEL_BUCKET }.toSortedMap()
    for ((bucket, levels) in buckets) {
        val first = bucket * LEVEL_BUCKET + 1
        val candidates = levels.sumOf { report.candidatesByLevel[it] ?: 0 }
        val rate = if (candidates > 0) {
            levels.sumOf { report.passesByLevel[it] ?: 0 }.toDouble() / candidates
        } else {
            levels.map { report.passRateByLevel.getValue(it) }.average()
        }
        val lastPaths = levels.sumOf { report.lastPathByLevel[it] ?: 0 }
        appendLine(
            "levels $first-${first + LEVEL_BUCKET - 1}: pass rate ${"%.3f".format(rate)}" +
                (if (candidates > 0) " ($candidates candidates)" else "") +
                ", last path $lastPaths",
        )
    }
}
