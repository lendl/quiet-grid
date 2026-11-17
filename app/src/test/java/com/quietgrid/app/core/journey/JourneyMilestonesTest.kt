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
    fun `the last missing star masters the journey`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(3), "b1" to cleared(3), "b2" to cleared(2))
        val after = before + ("b2" to cleared(3))
        assertEquals(listOf(JourneyMilestone.Mastered), journeyMilestones(file, "b2", before, after))
    }

    @Test
    fun `a third star with other stops still short does not master`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(2), "b1" to cleared(3), "b2" to cleared(2))
        val after = before + ("b2" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "b2", before, after))
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
        val before = mapOf("a1" to cleared(2), "a2" to cleared(3), "b1" to cleared(1), "b2" to cleared(2))
        val after = before + ("a1" to cleared(3))
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "a1", before, after))
    }

    @Test
    fun `the last missing star after the journey was finished masters it`() {
        val before = mapOf("a1" to cleared(2), "a2" to cleared(3), "b1" to cleared(3), "b2" to cleared(3))
        val after = before + ("a1" to cleared(3))
        assertEquals(listOf(JourneyMilestone.Mastered), journeyMilestones(file, "a1", before, after))
    }

    @Test
    fun `finishing everything with every star on one win fires all three`() {
        val before = mapOf("a1" to cleared(3), "a2" to cleared(3), "b1" to cleared(3))
        val after = before + ("b2" to cleared(3))
        assertEquals(
            listOf(JourneyMilestone.ChapterComplete(2), JourneyMilestone.JourneyComplete, JourneyMilestone.Mastered),
            journeyMilestones(file, "b2", before, after),
        )
    }

    @Test
    fun `the biggest milestone is the one celebrated`() {
        val chapter = JourneyMilestone.ChapterComplete(2)
        assertEquals(JourneyMilestone.Mastered, headlineMilestone(listOf(chapter, JourneyMilestone.JourneyComplete, JourneyMilestone.Mastered)))
        assertEquals(JourneyMilestone.JourneyComplete, headlineMilestone(listOf(chapter, JourneyMilestone.JourneyComplete)))
        assertEquals(chapter, headlineMilestone(listOf(chapter)))
        assertNull(headlineMilestone(emptyList()))
    }

    @Test
    fun `an unknown stop fires nothing`() {
        assertEquals(emptyList<JourneyMilestone>(), journeyMilestones(file, "zz", emptyMap(), mapOf("zz" to cleared(3))))
    }
}
