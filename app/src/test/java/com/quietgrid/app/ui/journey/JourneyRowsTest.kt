package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyRowsTest {
    private fun chapter(index: Int, stops: Int) = JourneyChapter(
        index = index,
        minRating = index * 100,
        maxRating = index * 100 + 99,
        stops = (1..stops).map { JourneyStop("c$index-s$it", "easy", index * 100 + it, 60) },
    )

    private val file = JourneyFile("animaldoku", listOf(chapter(1, 6), chapter(2, 5)))

    private fun states(rows: List<JourneyStopRow>, chapterIndex: Int) =
        rows.filter { it.chapterIndex == chapterIndex }.map { it.state }

    @Test
    fun `fresh journey marks stop 1 current, 2-4 open, rest later`() {
        val rows = journeyRows(file, emptyMap())
        assertEquals(
            listOf(JourneyStopState.CURRENT, JourneyStopState.OPEN, JourneyStopState.OPEN, JourneyStopState.OPEN, JourneyStopState.LATER, JourneyStopState.LATER),
            states(rows, 1),
        )
        assertEquals((1..6).toList(), rows.filter { it.chapterIndex == 1 }.map { it.number })
    }

    @Test
    fun `every chapter's first stops are open from the start`() {
        val rows = journeyRows(file, emptyMap())
        assertEquals(
            listOf(JourneyStopState.CURRENT, JourneyStopState.OPEN, JourneyStopState.OPEN, JourneyStopState.OPEN, JourneyStopState.LATER),
            states(rows, 2),
        )
    }

    @Test
    fun `cleared and skipped stops show as done and skipped`() {
        val progress = mapOf(
            "c1-s1" to JourneyStopProgress(stars = 2, cleared = true),
            "c1-s2" to JourneyStopProgress(skipped = true),
        )
        val rows = journeyRows(file, progress)
        assertEquals(
            listOf(JourneyStopState.DONE, JourneyStopState.SKIPPED, JourneyStopState.CURRENT, JourneyStopState.OPEN, JourneyStopState.OPEN, JourneyStopState.OPEN),
            states(rows, 1),
        )
        assertEquals(2, rows.first().progress.stars)
    }

    @Test
    fun `request for an uncleared stop after a loss plays the sibling`() {
        val rows = journeyRows(file, mapOf("c1-s1" to JourneyStopProgress(attempts = 1, nextPuzzleId = "sib")))
        assertEquals(JourneyRequest("c1-s1", "sib", 0), journeyRequestFor(rows.first()))
        assertEquals(JourneyRequest("c1-s2", "c1-s2", 0), journeyRequestFor(rows[1]))
    }

    @Test
    fun `request for a cleared stop plays the stop itself with transform 0 when none exist`() {
        val rows = journeyRows(file, mapOf("c1-s1" to JourneyStopProgress(stars = 1, cleared = true, attempts = 3, nextPuzzleId = null)))
        assertEquals(JourneyRequest("c1-s1", "c1-s1", 0), journeyRequestFor(rows.first()))
    }

    @Test
    fun `cleared stop request carries its next transform`() {
        val withTransforms = file.copy(
            chapters = file.chapters.map { chapter -> chapter.copy(stops = chapter.stops.map { it.copy(replayTransforms = listOf(2, 5)) }) },
        )
        val rows = journeyRows(withTransforms, mapOf("c1-s1" to JourneyStopProgress(stars = 1, cleared = true, attempts = 3)))
        assertEquals(JourneyRequest("c1-s1", "c1-s1", 5), journeyRequestFor(rows.first()))
    }

    @Test
    fun `cleared stop with a lined-up sibling replays the sibling untransformed`() {
        val withTransforms = file.copy(
            chapters = file.chapters.map { chapter -> chapter.copy(stops = chapter.stops.map { it.copy(replayTransforms = listOf(2, 5)) }) },
        )
        val rows = journeyRows(withTransforms, mapOf("c1-s1" to JourneyStopProgress(stars = 2, cleared = true, attempts = 3, nextPuzzleId = "sib")))
        assertEquals(JourneyRequest("c1-s1", "sib", 0), journeyRequestFor(rows.first()))
    }

    @Test
    fun `uncleared stop request never transforms`() {
        val withTransforms = file.copy(
            chapters = file.chapters.map { chapter -> chapter.copy(stops = chapter.stops.map { it.copy(replayTransforms = listOf(2, 5)) }) },
        )
        val rows = journeyRows(withTransforms, mapOf("c1-s1" to JourneyStopProgress(attempts = 2, nextPuzzleId = "sib")))
        assertEquals(JourneyRequest("c1-s1", "sib", 0), journeyRequestFor(rows.first()))
    }

    @Test
    fun `summary points at the first current stop and totals stars`() {
        val progress = mapOf(
            "c1-s1" to JourneyStopProgress(stars = 3, cleared = true),
            "c2-s1" to JourneyStopProgress(stars = 2, cleared = true),
        )
        val summary = journeySummary(file, progress)
        assertEquals(JourneySummary(chapterIndex = 1, stopNumber = 2, stars = 5, maxStars = 33), summary)

        val allCleared = file.chapters.flatMap { it.stops }.associate { it.puzzleId to JourneyStopProgress(stars = 1, cleared = true) }
        assertEquals(JourneySummary(chapterIndex = null, stopNumber = null, stars = 11, maxStars = 33), journeySummary(file, allCleared))
    }

    @Test
    fun `chapter summaries count each chapter's own stops and stars`() {
        val progress = mapOf(
            "c1-s1" to JourneyStopProgress(stars = 3, cleared = true),
            "c1-s2" to JourneyStopProgress(stars = 1, cleared = true),
        )
        val summaries = journeyChapterSummaries(journeyRows(file, progress))
        assertEquals(listOf(1, 2), summaries.map { it.index })
        assertEquals(listOf(6, 5), summaries.map { it.total })
        assertEquals(listOf(2, 0), summaries.map { it.cleared })
        assertEquals(listOf(4, 0), summaries.map { it.stars })
        assertEquals(listOf(18, 15), summaries.map { it.maxStars })
    }

    @Test
    fun `status follows cleared and skipped stops`() {
        val skippedOnly = journeyChapterSummaries(journeyRows(file, mapOf("c2-s3" to JourneyStopProgress(skipped = true))))
        assertEquals(listOf(JourneyChapterStatus.OPEN, JourneyChapterStatus.IN_PROGRESS), skippedOnly.map { it.status })

        val chapterOneCleared = file.chapters.first().stops.associate { it.puzzleId to JourneyStopProgress(stars = 1, cleared = true) }
        val complete = journeyChapterSummaries(journeyRows(file, chapterOneCleared))
        assertEquals(listOf(JourneyChapterStatus.COMPLETE, JourneyChapterStatus.OPEN), complete.map { it.status })

        val oneSkipped = chapterOneCleared + ("c1-s6" to JourneyStopProgress(skipped = true))
        assertEquals(JourneyChapterStatus.IN_PROGRESS, journeyChapterSummaries(journeyRows(file, oneSkipped)).first().status)
    }

    @Test
    fun `tiers list the chapter's difficulties in order`() {
        val mixed = JourneyChapter(
            index = 1,
            minRating = 0,
            maxRating = 99,
            stops = listOf("easy", "easy", "medium", "easy", "hard").mapIndexed { i, tier -> JourneyStop("m$i", tier, i, 60) },
        )
        val summaries = journeyChapterSummaries(journeyRows(JourneyFile("animaldoku", listOf(mixed)), emptyMap()))
        assertEquals(listOf("easy", "medium", "hard"), summaries.single().tiers)
    }
}
