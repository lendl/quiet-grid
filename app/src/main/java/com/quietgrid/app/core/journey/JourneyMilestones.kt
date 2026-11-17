package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyFile

sealed interface JourneyMilestone {
    data class ChapterComplete(val chapterIndex: Int) : JourneyMilestone
    data class ChapterMastered(val chapterIndex: Int) : JourneyMilestone
    data object JourneyComplete : JourneyMilestone
    data object Mastered : JourneyMilestone
}

val JourneyMilestone.rank: Int
    get() = when (this) {
        is JourneyMilestone.ChapterComplete -> 0
        is JourneyMilestone.ChapterMastered -> 1
        JourneyMilestone.JourneyComplete -> 2
        JourneyMilestone.Mastered -> 3
    }

val JourneyMilestone.chapterIndex: Int
    get() = when (this) {
        is JourneyMilestone.ChapterComplete -> chapterIndex
        is JourneyMilestone.ChapterMastered -> chapterIndex
        JourneyMilestone.JourneyComplete, JourneyMilestone.Mastered -> 0
    }

fun journeyMilestones(
    file: JourneyFile,
    stopId: String,
    before: Map<String, JourneyStopProgress>,
    after: Map<String, JourneyStopProgress>,
): List<JourneyMilestone> {
    val chapter = file.chapters.firstOrNull { chapter -> chapter.stops.any { it.puzzleId == stopId } } ?: return emptyList()
    val chapterIds = chapter.stops.map { it.puzzleId }
    val allIds = file.chapters.flatMap { it.stops }.map { it.puzzleId }
    fun Map<String, JourneyStopProgress>.cleared(id: String) = this[id]?.cleared == true
    fun Map<String, JourneyStopProgress>.full(id: String) = (this[id]?.stars ?: 0) >= JOURNEY_MAX_STARS_PER_STOP
    val chapterComplete = chapterIds.all { after.cleared(it) } && chapterIds.any { !before.cleared(it) }
    val chapterMastered = chapterIds.all { after.full(it) } && chapterIds.any { !before.full(it) }
    val journeyComplete = allIds.isNotEmpty() && allIds.all { after.cleared(it) } && allIds.any { !before.cleared(it) }
    val mastered = allIds.isNotEmpty() && allIds.all { after.full(it) } && allIds.any { !before.full(it) }
    return listOfNotNull(
        JourneyMilestone.ChapterComplete(chapter.index).takeIf { chapterComplete },
        JourneyMilestone.ChapterMastered(chapter.index).takeIf { chapterMastered },
        JourneyMilestone.JourneyComplete.takeIf { journeyComplete },
        JourneyMilestone.Mastered.takeIf { mastered },
    )
}

fun headlineMilestone(milestones: List<JourneyMilestone>): JourneyMilestone? = milestones.maxByOrNull { it.rank }
