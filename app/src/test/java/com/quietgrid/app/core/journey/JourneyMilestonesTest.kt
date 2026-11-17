package com.quietgrid.app.core.journey

import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyMilestonesTest {
    private fun chapter(index: Int, vararg ids: String) =
        JourneyChapter(index = index, minRating = 100, maxRating = 400, stops = ids.map { JourneyStop(it, "easy", 150, 90) })

    private val file = JourneyFile("animaldoku", listOf(chapter(1, "a1", "a2"), chapter(2, "b1", "b2")))

    private fun cleared(stars: Int) = JourneyStopProgress(stars = stars, cleared = true)

    @Test
    fun `clearing the last stop of a chapter completes it`() {
        val before = mapOf("a1" to cleared(2))
        val after = before + ("a2" to cleared(1))
        assertEquals(listOf(JourneyMilestone.ChapterComplete(1)), journeyMilestones(file, "a2", before, after))
    }

    @Test
    fun `replaying a stop in a finished chapter fires nothing`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(1))
        val after = before + ("a2" to cleared(2))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a2", before, after))
    }

    @Test
    fun `a skipped stop keeps the chapter open`() {
        val before = mapOf("a1" to JourneyStopProgress(skipped = true))
        val after = before + ("a2" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a2", before, after))
    }

    @Test
    fun `the last missing star masters the chapter and the journey`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(3), "b1" to cleared(3), "b2" to cleared(2))
        val after = before + ("b2" to cleared(3))
        assertEquals(
            listOf(JourneyMilestone.ChapterMastered(2), JourneyMilestone.Mastered),
            journeyMilestones(file, "b2", before, after),
        )
    }

    @Test
    fun `a third star with another chapter still short masters only its own chapter`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(2), "b1" to cleared(3), "b2" to cleared(2))
        val after = before + ("b2" to cleared(3))
        assertEquals(listOf(JourneyMilestone.ChapterMastered(2)), journeyMilestones(file, "b2", before, after))
    }

    @Test
    fun `a third star with its own chapter still short masters nothing`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(2))
        val after = before + ("a2" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a2", before, after))
    }

    @Test
    fun `finishing a chapter with every star completes and masters it`() {
        val before = mapOf("a1" to cleared(3))
        val after = before + ("a2" to cleared(3))
        assertEquals(
            listOf(JourneyMilestone.ChapterComplete(1), JourneyMilestone.ChapterMastered(1)),
            journeyMilestones(file, "a2", before, after),
        )
    }

    @Test
    fun `replaying a stop of a mastered chapter fires nothing`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a2", before, before))
    }

    @Test
    fun `clearing the last open stop finishes the journey with stars still missing`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(3), "b1" to cleared(1))
        val after = before + ("b2" to cleared(2))
        assertEquals(
            listOf(JourneyMilestone.ChapterComplete(2), JourneyMilestone.JourneyComplete),
            journeyMilestones(file, "b2", before, after),
        )
    }

    @Test
    fun `replaying a stop of a finished journey does not finish it again`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(2), "b1" to cleared(1), "b2" to cleared(2))
        val after = before + ("a1" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a1", before, after))
    }

    @Test
    fun `the last missing star after the journey was finished masters it`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(3), "b1" to cleared(3), "b2" to cleared(3))
        val after = before + ("a1" to cleared(3))
        assertEquals(
            listOf(JourneyMilestone.ChapterMastered(1), JourneyMilestone.Mastered),
            journeyMilestones(file, "a1", before, after),
        )
    }

    @Test
    fun `finishing everything with every star on one win fires all four`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(3), "b1" to cleared(3))
        val after = before + ("b2" to cleared(3))
        assertEquals(
            listOf(
                JourneyMilestone.ChapterComplete(2),
                JourneyMilestone.ChapterMastered(2),
                JourneyMilestone.JourneyComplete,
                JourneyMilestone.Mastered,
            ),
            journeyMilestones(file, "b2", before, after),
        )
    }

    @Test
    fun `the biggest milestone is the one celebrated`() {
        val chapter = JourneyMilestone.ChapterComplete(2)
        val chapterGold = JourneyMilestone.ChapterMastered(2)
        assertEquals(JourneyMilestone.Mastered, headlineMilestone(listOf(chapter, chapterGold, JourneyMilestone.JourneyComplete, JourneyMilestone.Mastered)))
        assertEquals(JourneyMilestone.JourneyComplete, headlineMilestone(listOf(chapter, chapterGold, JourneyMilestone.JourneyComplete)))
        assertEquals(chapterGold, headlineMilestone(listOf(chapter, chapterGold)))
        assertEquals(chapter, headlineMilestone(listOf(chapter)))
        assertNull(headlineMilestone(emptyList()))
    }

    @Test
    fun `an unknown stop fires nothing`() {
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "zz", emptyMap(), mapOf("zz" to cleared(3))))
    }
}
