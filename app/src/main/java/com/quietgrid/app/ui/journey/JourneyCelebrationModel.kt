package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JOURNEY_MAX_STARS_PER_STOP
import com.quietgrid.app.data.PlayRecord

data class ChapterCelebrationModel(
    val chapterIndex: Int,
    val stopCount: Int,
    val stars: Int,
    val maxStars: Int,
    val bestSeconds: Int,
    val cleanBoards: Int,
    val nextChapterStop: JourneyStopRow?,
    val replayStop: JourneyStopRow?,
)

data class JourneyCelebrationModel(
    val chapters: List<JourneyChapterSummary>,
    val stopCount: Int,
    val stars: Int,
    val maxStars: Int,
    val cleanBoards: Int,
    val replayStop: JourneyStopRow?,
) {
    val lastChapterIndex: Int get() = chapters.last().index
    val starsToGo: Int get() = maxStars - stars
}

fun chapterCelebrationModel(rows: List<JourneyStopRow>, chapterIndex: Int): ChapterCelebrationModel? {
    val chapterRows = rows.filter { it.chapterIndex == chapterIndex }
    if (chapterRows.isEmpty()) return null
    val chapterOrder = rows.map { it.chapterIndex }.distinct()
    val nextChapter = chapterOrder.getOrNull(chapterOrder.indexOf(chapterIndex) + 1)
    return ChapterCelebrationModel(
        chapterIndex = chapterIndex,
        stopCount = chapterRows.size,
        stars = chapterRows.sumOf { it.progress.stars },
        maxStars = chapterRows.size * JOURNEY_MAX_STARS_PER_STOP,
        bestSeconds = chapterRows.sumOf { it.progress.bestSeconds ?: 0 },
        cleanBoards = chapterRows.count { it.progress.clean },
        nextChapterStop = nextChapter?.let { next -> rows.firstOrNull { it.chapterIndex == next && it.state == JourneyStopState.CURRENT } },
        replayStop = chapterRows.firstOrNull { it.progress.stars < JOURNEY_MAX_STARS_PER_STOP },
    )
}

fun journeyCelebrationModel(rows: List<JourneyStopRow>): JourneyCelebrationModel? {
    if (rows.isEmpty()) return null
    return JourneyCelebrationModel(
        chapters = journeyChapterSummaries(rows),
        stopCount = rows.size,
        stars = rows.sumOf { it.progress.stars },
        maxStars = rows.size * JOURNEY_MAX_STARS_PER_STOP,
        cleanBoards = rows.count { it.progress.clean },
        replayStop = rows.firstOrNull { it.progress.stars < JOURNEY_MAX_STARS_PER_STOP },
    )
}

fun totalJourneySeconds(records: List<PlayRecord>): Int =
    records.filter { it.journeyStop != null }.sumOf { it.elapsedSeconds }
