package com.quietgrid.cli.journey

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.JOURNEY_RATING_MAX
import com.quietgrid.engine.journey.JOURNEY_RATING_MIN
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlin.math.abs
import kotlin.math.roundToInt

const val JOURNEY_STOPS_PER_CHAPTER = 60
const val JOURNEY_MAX_STOPS_PER_CHAPTER = 70
const val JOURNEY_PUZZLES_PER_BAND = 120
const val JOURNEY_MAX_CHAPTERS = 10
const val JOURNEY_MIN_CHAPTERS = 4

data class RatedPuzzle(val id: String, val difficulty: String, val rating: Int)

private val byRatingThenId = compareBy<RatedPuzzle>({ it.rating }, { it.id })

fun journeyBands(puzzleCount: Int): List<IntRange> {
    val count = (puzzleCount / JOURNEY_PUZZLES_PER_BAND).coerceIn(JOURNEY_MIN_CHAPTERS, JOURNEY_MAX_CHAPTERS)
    val span = JOURNEY_RATING_MAX - JOURNEY_RATING_MIN
    val edges = (0..count).map { JOURNEY_RATING_MIN + (it * span / count.toDouble()).roundToInt() }
    return (0 until count).map { i -> edges[i]..if (i == count - 1) JOURNEY_RATING_MAX else edges[i + 1] - 1 }
}

fun journeyStopCount(bandSize: Int, keepSiblings: Boolean): Int =
    minOf(JOURNEY_STOPS_PER_CHAPTER, if (keepSiblings) (bandSize + 1) / 2 else bandSize)

private fun spreadStops(puzzles: List<RatedPuzzle>, keepSiblings: Boolean): List<RatedPuzzle> {
    val sorted = puzzles.sortedWith(byRatingThenId)
    val count = journeyStopCount(sorted.size, keepSiblings)
    if (count >= sorted.size) return sorted
    if (count == 1) return listOf(sorted[sorted.size / 2])
    val last = count - 1
    return (0..last).map { sorted[(it * (sorted.size - 1) / last.toDouble()).roundToInt()] }
}

private fun RatedPuzzle.toStop(target: JourneyTargetSeconds, replaces: String? = null): JourneyStop =
    JourneyStop(id, difficulty, rating, targetSecondsFor(rating, target), replaces)

private fun chaptersFor(
    bands: List<IntRange>,
    puzzles: List<RatedPuzzle>,
    firstIndex: Int,
    target: JourneyTargetSeconds,
    keepSiblings: Boolean,
): List<JourneyChapter> =
    bands.mapNotNull { band -> puzzles.filter { it.rating in band }.takeIf { it.isNotEmpty() }?.let { band to it } }
        .mapIndexed { offset, (band, inBand) ->
            JourneyChapter(firstIndex + offset, band.first, band.last, spreadStops(inBand, keepSiblings).map { it.toStop(target) })
        }

fun buildJourney(game: String, puzzles: List<RatedPuzzle>, target: JourneyTargetSeconds, keepSiblings: Boolean = false): JourneyFile =
    JourneyFile(game, chaptersFor(journeyBands(puzzles.size), puzzles, 1, target, keepSiblings))

fun mergeJourney(
    existing: JourneyFile,
    puzzles: List<RatedPuzzle>,
    newIds: Set<String>,
    target: JourneyTargetSeconds,
    keepSiblings: Boolean = false,
): JourneyFile {
    if (existing.chapters.isEmpty()) return buildJourney(existing.game, puzzles, target, keepSiblings)
    val byId = puzzles.associateBy { it.id }
    val used = existing.chapters.flatMap { chapter -> chapter.stops.map { it.puzzleId } }.filter { it in byId }.toMutableSet()
    val merged = existing.chapters.map { chapter ->
        val band = chapter.minRating..chapter.maxRating
        val stops = chapter.stops.mapNotNull { stop ->
            byId[stop.puzzleId]?.let { return@mapNotNull it.toStop(target, stop.replaces) }
            val replacement = puzzles
                .filter { it.rating in band && it.id !in used }
                .minWithOrNull(compareBy<RatedPuzzle>({ abs(it.rating - stop.rating) }, { it.rating }, { it.id }))
                ?: return@mapNotNull null
            used += replacement.id
            replacement.toStop(target, stop.puzzleId)
        }.toMutableList()
        val fresh = puzzles.filter { it.id in newIds && it.rating in band && it.id !in used }.toMutableList()
        while (stops.size < JOURNEY_MAX_STOPS_PER_CHAPTER && fresh.isNotEmpty()) {
            val gap = { puzzle: RatedPuzzle -> stops.minOfOrNull { abs(it.rating - puzzle.rating) } ?: Int.MAX_VALUE }
            val pick = fresh.maxWith(compareBy<RatedPuzzle>(gap).thenByDescending { it.rating }.thenByDescending { it.id })
            fresh -= pick
            used += pick.id
            stops += pick.toStop(target)
        }
        chapter.copy(stops = stops)
    }
    val top = merged.last()
    val width = top.maxRating - top.minRating + 1
    val above = puzzles.filter { it.rating > top.maxRating && it.id !in used }
    val highest = above.maxOfOrNull { it.rating } ?: return existing.copy(chapters = merged)
    val bands = generateSequence(top.maxRating + 1) { it + width }
        .takeWhile { it <= highest }
        .map { it until it + width }
        .toList()
    return existing.copy(chapters = merged + chaptersFor(bands, above, top.index + 1, target, keepSiblings))
}

const val JOURNEY_REPLAY_RATING_TOLERANCE = 0.05

fun allowedReplayTransforms(candidates: List<GridTransform>, stopRating: Int, rate: (GridTransform) -> Int?): List<Int> =
    candidates.filter { t ->
        val rating = rate(t) ?: return@filter false
        abs(rating - stopRating) <= stopRating * JOURNEY_REPLAY_RATING_TOLERANCE
    }.map { it.id }

fun withReplayTransforms(file: JourneyFile, transformsFor: (JourneyStop) -> List<Int>): JourneyFile =
    file.copy(chapters = file.chapters.map { chapter -> chapter.copy(stops = chapter.stops.map { it.copy(replayTransforms = transformsFor(it)) }) })

fun validateJourney(file: JourneyFile, bankIds: Set<String>) {
    val seen = mutableSetOf<String>()
    file.chapters.forEach { chapter ->
        check(chapter.stops.size <= JOURNEY_MAX_STOPS_PER_CHAPTER) { "Chapter ${chapter.index} has ${chapter.stops.size} stops" }
        chapter.stops.forEach { stop ->
            check(stop.puzzleId in bankIds) { "Stop ${stop.puzzleId} in chapter ${chapter.index} is not in the ${file.game} bank" }
            check(seen.add(stop.puzzleId)) { "Stop ${stop.puzzleId} appears more than once in ${file.game}" }
        }
    }
}
