package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyCelebrationModelTest {
    private fun chapter(index: Int, vararg ids: String) =
        JourneyChapter(index = index, minRating = 100, maxRating = 400, stops = ids.map { JourneyStop(it, "easy", 150, 90) })

    private val file = JourneyFile("animaldoku", listOf(chapter(1, "a1", "a2", "a3"), chapter(2, "b1", "b2")))

    private fun won(stars: Int, seconds: Int, clean: Boolean = false) =
        JourneyStopProgress(stars = stars, cleared = true, clean = clean, bestSeconds = seconds)

    @Test
    fun `chapter summary adds up stars, times and clean boards`() {
        val progress = mapOf("a1" to won(3, 60, clean = true), "a2" to won(1, 100), "a3" to won(2, 40, clean = true))
        val model = chapterCelebrationModel(journeyRows(file, progress), 1)!!

        assertEquals(3, model.stopCount)
        assertEquals(3, model.clearedCount)
        assertEquals(6, model.stars)
        assertEquals(9, model.maxStars)
        assertEquals(200, model.bestSeconds)
        assertEquals(2, model.cleanBoards)
        assertEquals(2, model.canEarnMore)
        assertEquals("a2", model.replayStop?.stop?.puzzleId)
        assertEquals("b1", model.nextChapterStop?.stop?.puzzleId)
    }

    @Test
    fun `next chapter start skips settled stops`() {
        val progress = mapOf(
            "a1" to won(3, 60),
            "a2" to won(3, 60),
            "a3" to won(3, 60),
            "b1" to JourneyStopProgress(skipped = true),
        )
        val model = chapterCelebrationModel(journeyRows(file, progress), 1)!!

        assertEquals("b2", model.nextChapterStop?.stop?.puzzleId)
        assertEquals(0, model.canEarnMore)
        assertNull(model.replayStop)
    }

    @Test
    fun `the last chapter has no next stop`() {
        val progress = mapOf("b1" to won(3, 60), "b2" to won(2, 60))
        val model = chapterCelebrationModel(journeyRows(file, progress), 2)!!

        assertNull(model.nextChapterStop)
        assertEquals("b2", model.replayStop?.stop?.puzzleId)
    }

    @Test
    fun `an unknown chapter has no model`() {
        assertNull(chapterCelebrationModel(journeyRows(file, emptyMap()), 9))
    }

    @Test
    fun `mastered summary covers the whole journey`() {
        val progress = listOf("a1", "a2", "a3", "b1", "b2").associateWith { won(3, 3600) }
        val model = masteredCelebrationModel(journeyRows(file, progress))!!

        assertEquals(listOf(1, 2), model.chapterIndexes)
        assertEquals(5, model.stopCount)
        assertEquals(15, model.stars)
        assertEquals(5 * 3600, model.bestSeconds)
        assertEquals(2, model.lastChapterIndex)
    }
}
