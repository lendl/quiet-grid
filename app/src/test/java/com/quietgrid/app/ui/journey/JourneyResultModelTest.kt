package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyResultModelTest {
    private fun chapter(index: Int, stops: Int) = JourneyChapter(
        index = index,
        minRating = index * 100,
        maxRating = index * 100 + 99,
        stops = (1..stops).map { JourneyStop("c$index-s$it", "easy", index * 100 + it, 90) },
    )

    private val file = JourneyFile("animaldoku", listOf(chapter(1, 6), chapter(2, 5)))

    private val clean = JourneySignals(hintsUsed = 0, mistakes = 0)

    private fun cleared(stars: Int) = JourneyStopProgress(stars = stars, cleared = true, attempts = 1)

    @Test
    fun `clean fast run earns three stars and all goals met`() {
        val rows = journeyRows(file, mapOf("c1-s1" to cleared(3)))
        val model = journeyResultModel(rows, "c1-s1", elapsedSeconds = 60, signals = clean)!!
        assertEquals(3, model.runStars)
        assertEquals(3, model.bestStars)
        assertEquals(1, model.chapterIndex)
        assertEquals(1, model.stopNumber)
        assertEquals(90, model.targetSeconds)
        assertEquals(
            listOf(JourneyGoal(JourneyGoalKind.CLEAR, true), JourneyGoal(JourneyGoalKind.CLEAN, true), JourneyGoal(JourneyGoalKind.TIME, true)),
            model.goals,
        )
        assertNull(model.replay)
    }

    @Test
    fun `hinted slow run earns one star, best stays three`() {
        val rows = journeyRows(file, mapOf("c1-s1" to cleared(3)))
        val model = journeyResultModel(rows, "c1-s1", elapsedSeconds = 200, signals = JourneySignals(hintsUsed = 1, mistakes = 0))!!
        assertEquals(1, model.runStars)
        assertEquals(3, model.bestStars)
        assertEquals(listOf(true, false, false), model.goals.map { it.met })
        assertNull(model.replay)
    }

    @Test
    fun `replay offered below three stars`() {
        val rows = journeyRows(file, mapOf("c1-s2" to cleared(2)))
        val model = journeyResultModel(rows, "c1-s2", elapsedSeconds = 200, signals = clean)!!
        assertEquals(2, model.runStars)
        assertEquals("c1-s2", model.replay?.stop?.puzzleId)
    }

    @Test
    fun `next stop is the first current or open stop after this one`() {
        val progress = mapOf("c1-s1" to cleared(3), "c1-s2" to JourneyStopProgress(skipped = true), "c1-s3" to cleared(1))
        val rows = journeyRows(file, progress)
        assertEquals("c1-s4", journeyResultModel(rows, "c1-s1", 60, clean)?.next?.stop?.puzzleId)

        val endOfChapter = journeyRows(file, (1..6).associate { "c1-s$it" to cleared(1) })
        assertEquals("c2-s1", journeyResultModel(endOfChapter, "c1-s6", 60, clean)?.next?.stop?.puzzleId)
    }

    @Test
    fun `last stop has no next stop`() {
        val rows = journeyRows(file, file.chapters.flatMap { it.stops }.associate { it.puzzleId to cleared(1) })
        assertNull(journeyResultModel(rows, "c2-s5", 60, clean)?.next)
    }

    @Test
    fun `unknown stop gives null`() {
        assertNull(journeyResultModel(journeyRows(file, emptyMap()), "missing", 60, clean))
    }
}
