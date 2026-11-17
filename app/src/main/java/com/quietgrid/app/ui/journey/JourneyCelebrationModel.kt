package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JOURNEY_MAX_STARS_PER_STOP
import com.quietgrid.app.data.PlayRecord

data class ChapterCelebrationModel(
    val chapterIndex: Int,
    val stopCount: Int,
    val stars: Int,
    val maxStars: Int,
    val playSeconds: Int,
    val cleanBoards: Int,
    val nextChapterStop: JourneyStopRow?,
    val replayStop: JourneyStopRow?,
)

data class JourneyCelebrationModel(
    val chapters: List<JourneyChapterSummary>,
    val stopCount: Int,
    val stars: Int,
    val maxStars: Int,
    val playSeconds: Int,
    val cleanBoards: Int,
    val replayStop: JourneyStopRow?,
) {
    val lastChapterIndex: Int get() = chapters.last().index
    val starsToGo: Int get() = maxStars - stars
}

fun chapterCelebrationModel(rows: List<JourneyStopRow>, chapterIndex: Int, secondsByStop: Map<String, Int>): ChapterCelebrationModel? {
    val chapterRows = rows.filter { it.chapterIndex == chapterIndex }
    if (chapterRows.isEmpty()) return null
    val chapterOrder = rows.map { it.chapterIndex }.distinct()
    val nextChapter = chapterOrder.getOrNull(chapterOrder.indexOf(chapterIndex) + 1)
    return ChapterCelebrationModel(
        chapterIndex = chapterIndex,
        stopCount = chapterRows.size,
        stars = chapterRows.sumOf { it.progress.stars },
        maxStars = chapterRows.size * JOURNEY_MAX_STARS_PER_STOP,
        playSeconds = chapterRows.playSeconds(secondsByStop),
        cleanBoards = chapterRows.count { it.progress.clean },
        nextChapterStop = nextChapter?.let { next -> rows.firstOrNull { it.chapterIndex == next && it.state == JourneyStopState.CURRENT } },
        replayStop = chapterRows.firstOrNull { it.progress.stars < JOURNEY_MAX_STARS_PER_STOP },
    )
}

fun journeyCelebrationModel(rows: List<JourneyStopRow>, secondsByStop: Map<String, Int>): JourneyCelebrationModel? {
    if (rows.isEmpty()) return null
    return JourneyCelebrationModel(
        chapters = journeyChapterSummaries(rows),
        stopCount = rows.size,
        stars = rows.sumOf { it.progress.stars },
        maxStars = rows.size * JOURNEY_MAX_STARS_PER_STOP,
        playSeconds = rows.playSeconds(secondsByStop),
        cleanBoards = rows.count { it.progress.clean },
        replayStop = rows.firstOrNull { it.progress.stars < JOURNEY_MAX_STARS_PER_STOP },
    )
}

fun journeySecondsByStop(records: List<PlayRecord>): Map<String, Int> =
    records.mapNotNull { record -> record.journeyStop?.let { it to record.elapsedSeconds } }
        .groupingBy { it.first }
        .fold(0) { total, run -> total + run.second }

private fun List<JourneyStopRow>.playSeconds(secondsByStop: Map<String, Int>): Int =
    sumOf { row -> listOfNotNull(row.stop.puzzleId, row.stop.replaces).sumOf { secondsByStop[it] ?: 0 } }
