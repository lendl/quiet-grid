package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyFile

sealed interface JourneyMilestone {
    data class ChapterComplete(val chapterIndex: Int) : JourneyMilestone
    data object Mastered : JourneyMilestone
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
    val mastered = allIds.isNotEmpty() && allIds.all { after.full(it) } && allIds.any { !before.full(it) }
    return listOfNotNull(
        JourneyMilestone.ChapterComplete(chapter.index).takeIf { chapterComplete },
        JourneyMilestone.Mastered.takeIf { mastered },
    )
}
