package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JOURNEY_MAX_STARS_PER_STOP

data class ChapterCelebrationModel(
    val chapterIndex: Int,
    val stopCount: Int,
    val clearedCount: Int,
    val stars: Int,
    val maxStars: Int,
    val bestSeconds: Int,
    val cleanBoards: Int,
    val canEarnMore: Int,
    val nextChapterStop: JourneyStopRow?,
    val replayStop: JourneyStopRow?,
)

data class MasteredCelebrationModel(
    val chapterIndexes: List<Int>,
    val stopCount: Int,
    val stars: Int,
    val bestSeconds: Int,
) {
    val lastChapterIndex: Int get() = chapterIndexes.last()
}

fun chapterCelebrationModel(rows: List<JourneyStopRow>, chapterIndex: Int): ChapterCelebrationModel? {
    val chapterRows = rows.filter { it.chapterIndex == chapterIndex }
    if (chapterRows.isEmpty()) return null
    val chapterOrder = rows.map { it.chapterIndex }.distinct()
    val nextChapter = chapterOrder.getOrNull(chapterOrder.indexOf(chapterIndex) + 1)
    val shortOfStars = chapterRows.filter { it.progress.stars < JOURNEY_MAX_STARS_PER_STOP }
    return ChapterCelebrationModel(
        chapterIndex = chapterIndex,
        stopCount = chapterRows.size,
        clearedCount = chapterRows.count { it.progress.cleared },
        stars = chapterRows.sumOf { it.progress.stars },
        maxStars = chapterRows.size * JOURNEY_MAX_STARS_PER_STOP,
        bestSeconds = chapterRows.sumOf { it.progress.bestSeconds ?: 0 },
        cleanBoards = chapterRows.count { it.progress.clean },
        canEarnMore = shortOfStars.size,
        nextChapterStop = nextChapter?.let { next -> rows.firstOrNull { it.chapterIndex == next && it.state == JourneyStopState.CURRENT } },
        replayStop = shortOfStars.firstOrNull(),
    )
}

fun masteredCelebrationModel(rows: List<JourneyStopRow>): MasteredCelebrationModel? {
    if (rows.isEmpty()) return null
    return MasteredCelebrationModel(
        chapterIndexes = rows.map { it.chapterIndex }.distinct(),
        stopCount = rows.size,
        stars = rows.sumOf { it.progress.stars },
        bestSeconds = rows.sumOf { it.progress.bestSeconds ?: 0 },
    )
}
