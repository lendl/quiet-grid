package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyStop
import kotlin.math.abs
import kotlin.random.Random

const val JOURNEY_PLAYABLE_WINDOW = 4

data class JourneyStopProgress(
    val stars: Int = 0,
    val cleared: Boolean = false,
    val clean: Boolean = false,
    val underTarget: Boolean = false,
    val bestSeconds: Int? = null,
    val skipped: Boolean = false,
    val attempts: Int = 0,
    val nextPuzzleId: String? = null,
)

data class JourneySignals(val hintsUsed: Int, val mistakes: Int)

private fun JourneyStopProgress?.isSettled(): Boolean = this != null && (cleared || skipped)

fun playableStops(chapter: JourneyChapter, progress: Map<String, JourneyStopProgress>): Set<String> {
    val (settled, open) = chapter.stops.partition { progress[it.puzzleId].isSettled() }
    return (settled + open.take(JOURNEY_PLAYABLE_WINDOW)).map { it.puzzleId }.toSet()
}

fun currentStop(chapter: JourneyChapter, progress: Map<String, JourneyStopProgress>): JourneyStop? =
    chapter.stops.firstOrNull { !progress[it.puzzleId].isSettled() }

fun applyJourneyWin(old: JourneyStopProgress, signals: JourneySignals, elapsedSeconds: Int, targetSeconds: Int): JourneyStopProgress {
    val clean = signals.hintsUsed == 0 && signals.mistakes == 0
    val underTarget = elapsedSeconds < targetSeconds
    val stars = 1 + (if (clean) 1 else 0) + (if (underTarget) 1 else 0)
    return old.copy(
        stars = maxOf(old.stars, stars),
        cleared = true,
        clean = old.clean || clean,
        underTarget = old.underTarget || underTarget,
        bestSeconds = old.bestSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds,
        attempts = old.attempts + 1,
        nextPuzzleId = null,
    )
}

fun applyJourneyLoss(old: JourneyStopProgress, siblingId: String): JourneyStopProgress =
    old.copy(attempts = old.attempts + 1, nextPuzzleId = siblingId)

fun pickJourneySibling(
    stop: JourneyStop,
    chapter: JourneyChapter,
    pool: List<Pair<String, Int>>,
    playedIds: Set<String>,
    seed: Int,
): String {
    val stopIds = chapter.stops.map { it.puzzleId }.toSet()
    val candidates = pool.filter { (id, rating) -> id !in stopIds && rating in chapter.minRating..chapter.maxRating }
    val choices = candidates.filter { it.first !in playedIds }.ifEmpty { candidates }
    val nearest = choices.minOfOrNull { abs(it.second - stop.rating) } ?: return stop.puzzleId
    val tied = choices.filter { abs(it.second - stop.rating) == nearest }.map { it.first }.distinct().sorted()
    return tied[Random(seed).nextInt(tied.size)]
}

fun replayTransform(attempts: Int, transforms: List<Int>): Int =
    if (transforms.isEmpty()) 0 else transforms[attempts % transforms.size]

fun journeySiblingSeed(scope: String, stopId: String, attempts: Int): Int = "$scope|$stopId|$attempts".hashCode()
