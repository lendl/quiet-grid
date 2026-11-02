package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.journey.currentStop
import com.quietgrid.app.core.journey.playableStops
import com.quietgrid.app.core.journey.replayTransform
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop

enum class JourneyStopState { DONE, SKIPPED, CURRENT, OPEN, LATER }

data class JourneyStopRow(
    val chapterIndex: Int,
    val number: Int,
    val stop: JourneyStop,
    val progress: JourneyStopProgress,
    val state: JourneyStopState,
)

data class JourneySummary(val chapterIndex: Int?, val stopNumber: Int?, val stars: Int, val maxStars: Int)

const val JOURNEY_MAX_STARS_PER_STOP = 3

fun journeyRows(file: JourneyFile, progress: Map<String, JourneyStopProgress>): List<JourneyStopRow> =
    file.chapters.flatMap { chapter ->
        val playable = playableStops(chapter, progress)
        val current = currentStop(chapter, progress)?.puzzleId
        chapter.stops.mapIndexed { i, stop ->
            val stopProgress = progress[stop.puzzleId] ?: JourneyStopProgress()
            val state = when {
                stopProgress.cleared -> JourneyStopState.DONE
                stopProgress.skipped -> JourneyStopState.SKIPPED
                stop.puzzleId == current -> JourneyStopState.CURRENT
                stop.puzzleId in playable -> JourneyStopState.OPEN
                else -> JourneyStopState.LATER
            }
            JourneyStopRow(chapter.index, i + 1, stop, stopProgress, state)
        }
    }

fun journeyRequestFor(row: JourneyStopRow): JourneyRequest =
    if (row.progress.cleared) {
        JourneyRequest(row.stop.puzzleId, row.stop.puzzleId, replayTransform(row.progress.attempts, row.stop.replayTransforms))
    } else {
        JourneyRequest(row.stop.puzzleId, row.progress.nextPuzzleId ?: row.stop.puzzleId, 0)
    }

enum class JourneyChapterStatus { COMPLETE, IN_PROGRESS, OPEN }

data class JourneyChapterSummary(
    val index: Int,
    val tiers: List<String>,
    val stars: Int,
    val maxStars: Int,
    val cleared: Int,
    val total: Int,
    val status: JourneyChapterStatus,
)

fun journeyChapterSummaries(rows: List<JourneyStopRow>): List<JourneyChapterSummary> =
    rows.groupBy { it.chapterIndex }.map { (index, chapterRows) ->
        val cleared = chapterRows.count { it.progress.cleared }
        val status = when {
            cleared == chapterRows.size -> JourneyChapterStatus.COMPLETE
            chapterRows.any { it.progress.cleared || it.progress.skipped } -> JourneyChapterStatus.IN_PROGRESS
            else -> JourneyChapterStatus.OPEN
        }
        JourneyChapterSummary(
            index = index,
            tiers = chapterRows.map { it.stop.difficulty }.distinct(),
            stars = chapterRows.sumOf { it.progress.stars },
            maxStars = chapterRows.size * JOURNEY_MAX_STARS_PER_STOP,
            cleared = cleared,
            total = chapterRows.size,
            status = status,
        )
    }

fun journeySummary(file: JourneyFile, progress: Map<String, JourneyStopProgress>): JourneySummary {
    val rows = journeyRows(file, progress)
    val current = rows.firstOrNull { it.state == JourneyStopState.CURRENT }
    return JourneySummary(
        chapterIndex = current?.chapterIndex,
        stopNumber = current?.number,
        stars = rows.sumOf { it.progress.stars },
        maxStars = rows.size * JOURNEY_MAX_STARS_PER_STOP,
    )
}
