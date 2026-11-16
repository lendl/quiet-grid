package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyStop
import kotlin.math.abs

const val JOURNEY_PLAYABLE_WINDOW = 4
const val JOURNEY_MAX_STARS_PER_STOP = 3
const val JOURNEY_MIN_REPLAYS_PER_LEVEL = 4

data class JourneyStopProgress(
    val stars: Int = 0,
    val cleared: Boolean = false,
    val clean: Boolean = false,
    val underTarget: Boolean = false,
    val bestSeconds: Int? = null,
    val bestScore: Int? = null,
    val skipped: Boolean = false,
    val attempts: Int = 0,
    val nextPuzzleId: String? = null,
    val retired: List<String> = emptyList(),
)

data class JourneySignals(val hintsUsed: Int, val mistakes: Int)

data class JourneyRun(val clean: Boolean, val underTarget: Boolean) {
    val stars: Int get() = 1 + (if (clean) 1 else 0) + (if (underTarget) 1 else 0)
}

fun journeyRun(signals: JourneySignals, elapsedSeconds: Int, targetSeconds: Int): JourneyRun = JourneyRun(
    clean = signals.hintsUsed == 0 && signals.mistakes == 0,
    underTarget = elapsedSeconds < targetSeconds,
)

private fun JourneyStopProgress?.isSettled(): Boolean = this != null && (cleared || skipped)

fun playableStops(chapter: JourneyChapter, progress: Map<String, JourneyStopProgress>): Set<String> {
    val (settled, open) = chapter.stops.partition { progress[it.puzzleId].isSettled() }
    return (settled + open.take(JOURNEY_PLAYABLE_WINDOW)).map { it.puzzleId }.toSet()
}

fun currentStop(chapter: JourneyChapter, progress: Map<String, JourneyStopProgress>): JourneyStop? =
    chapter.stops.firstOrNull { !progress[it.puzzleId].isSettled() }

fun applyJourneyWin(old: JourneyStopProgress, run: JourneyRun, elapsedSeconds: Int, score: Int, replayPuzzleId: String? = null): JourneyStopProgress =
    old.copy(
        stars = maxOf(old.stars, run.stars),
        cleared = true,
        clean = old.clean || run.clean,
        underTarget = old.underTarget || run.underTarget,
        bestSeconds = old.bestSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds,
        bestScore = old.bestScore?.let { maxOf(it, score) } ?: score,
        attempts = old.attempts + 1,
        nextPuzzleId = replayPuzzleId,
    )

fun applyJourneyLoss(old: JourneyStopProgress, siblingId: String): JourneyStopProgress =
    old.copy(attempts = old.attempts + 1, nextPuzzleId = siblingId)

data class JourneyLevelRange(val stopId: String, val puzzles: List<Pair<String, Int>>) {
    val minRating: Int get() = puzzles.first().second
    val maxRating: Int get() = puzzles.last().second
}

private val byRatingThenId = compareBy<Pair<String, Int>>({ it.second }, { it.first })

fun journeyLevelRanges(stops: List<JourneyStop>, pool: List<Pair<String, Int>>): List<JourneyLevelRange> {
    if (stops.isEmpty()) return emptyList()
    val stopIds = stops.map { it.puzzleId }.toSet()
    val levels = stops.map { it.puzzleId to it.rating }.distinctBy { it.first }.sortedWith(byRatingThenId)
    val spares = pool.filter { it.first !in stopIds }.distinctBy { it.first }.sortedWith(byRatingThenId)
    val below = levels.map { -spares.binarySearch(it, byRatingThenId) - 1 }
    val share = minOf(JOURNEY_MIN_REPLAYS_PER_LEVEL, spares.size / levels.size)
    val edges = (1 until levels.size).runningFold(0) { previous, level ->
        ((below[level - 1] + below[level]) / 2)
            .coerceAtLeast(previous + share)
            .coerceAtMost(spares.size - (levels.size - level) * share)
    } + spares.size
    return levels.indices.map { level ->
        JourneyLevelRange(levels[level].first, (spares.subList(edges[level], edges[level + 1]) + levels[level]).sortedWith(byRatingThenId))
    }
}

fun pickJourneyReplay(
    stopId: String,
    ranges: List<JourneyLevelRange>,
    retiredByStop: Map<String, List<String>>,
    reservedElsewhere: Set<String>,
): String? {
    val level = ranges.indexOfFirst { it.stopId == stopId }
    if (level < 0) return null
    val ownRetired = retiredByStop[stopId].orEmpty()
    val previous = ownRetired.lastOrNull()
    val retiredAnywhere = retiredByStop.values.flatten().toSet()
    val stopIds = ranges.map { it.stopId }.toSet()
    val ranked = ranges.flatMapIndexed { index, range -> range.puzzles.map { it.first to index } }
    val home = ranked.indexOfFirst { it.first == stopId }
    val nearest = { allowed: (String) -> Boolean ->
        ranked.withIndex()
            .filter { (_, entry) -> entry.first != previous && entry.first !in reservedElsewhere && allowed(entry.first) }
            .minWithOrNull(compareBy({ it.value.second != level }, { abs(it.index - home) }, { it.index }))
            ?.value?.first
    }
    val ownRange = ranges[level].puzzles.map { it.first }
    val leastRecent = { ids: List<String> ->
        ids.filter { it != previous && it !in reservedElsewhere && (it == stopId || it !in stopIds) }.minByOrNull { ownRetired.lastIndexOf(it) }
    }
    val fresh = nearest { it !in retiredAnywhere && it !in stopIds }
    val recycleOwnFirst = ownRange.size > JOURNEY_MIN_REPLAYS_PER_LEVEL
    return fresh?.takeIf { it in ownRange }
        ?: leastRecent(ownRange).takeIf { recycleOwnFirst }
        ?: fresh
        ?: leastRecent(ownRetired.distinct())
        ?: nearest { it in retiredAnywhere }
}

fun replayTransform(attempts: Int, transforms: List<Int>): Int =
    if (transforms.isEmpty()) 0 else transforms[attempts % transforms.size]
