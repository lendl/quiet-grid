package com.quietgrid.app.ui.journey

import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.data.PlayRecord
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

    private fun record(seconds: Int, stop: String?) =
        PlayRecord(gameId = "animaldoku", difficulty = "easy", solved = true, score = 0, elapsedSeconds = seconds, timestampMillis = 0L, journeyStop = stop)

    @Test
    fun `chapter summary adds up stars, play time and clean boards`() {
        val progress = mapOf("a1" to won(3, 60, clean = true), "a2" to won(1, 100), "a3" to won(2, 40, clean = true))
        val seconds = mapOf("a1" to 150, "a2" to 100, "b1" to 999)
        val model = chapterCelebrationModel(journeyRows(file, progress), 1, seconds)!!

        assertEquals(3, model.stopCount)
        assertEquals(6, model.stars)
        assertEquals(9, model.maxStars)
        assertEquals(250, model.playSeconds)
        assertEquals(2, model.cleanBoards)
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
        val model = chapterCelebrationModel(journeyRows(file, progress), 1, emptyMap())!!

        assertEquals("b2", model.nextChapterStop?.stop?.puzzleId)
        assertNull(model.replayStop)
    }

    @Test
    fun `the last chapter has no next stop`() {
        val progress = mapOf("b1" to won(3, 60), "b2" to won(2, 60))
        val model = chapterCelebrationModel(journeyRows(file, progress), 2, emptyMap())!!

        assertNull(model.nextChapterStop)
        assertEquals("b2", model.replayStop?.stop?.puzzleId)
    }

    @Test
    fun `an unknown chapter has no model`() {
        assertNull(chapterCelebrationModel(journeyRows(file, emptyMap()), 9, emptyMap()))
    }

    @Test
    fun `journey summary covers every chapter and finds stars still to earn`() {
        val progress = mapOf(
            "a1" to won(3, 60, clean = true),
            "a2" to won(3, 60, clean = true),
            "a3" to won(3, 60),
            "b1" to won(2, 60),
            "b2" to won(3, 60, clean = true),
        )
        val seconds = mapOf("a1" to 100, "b2" to 50, "gone" to 999)
        val model = journeyCelebrationModel(journeyRows(file, progress), seconds)!!

        assertEquals(listOf(1, 2), model.chapters.map { it.index })
        assertEquals(listOf(9, 5), model.chapters.map { it.stars })
        assertEquals(5, model.stopCount)
        assertEquals(14, model.stars)
        assertEquals(15, model.maxStars)
        assertEquals(1, model.starsToGo)
        assertEquals(3, model.cleanBoards)
        assertEquals(150, model.playSeconds)
        assertEquals("b1", model.replayStop?.stop?.puzzleId)
        assertEquals(2, model.lastChapterIndex)
    }

    @Test
    fun `a journey with every star has nothing to replay`() {
        val progress = listOf("a1", "a2", "a3", "b1", "b2").associateWith { won(3, 60) }
        val model = journeyCelebrationModel(journeyRows(file, progress), emptyMap())!!

        assertEquals(0, model.starsToGo)
        assertNull(model.replayStop)
    }

    @Test
    fun `journey play time adds up every run per stop and skips other play`() {
        val records = listOf(record(60, "a1"), record(90, "a1"), record(500, null), record(30, "b2"))

        assertEquals(mapOf("a1" to 150, "b2" to 30), journeySecondsByStop(records))
    }

    @Test
    fun `play time counts runs saved under a stop's replaced id`() {
        val renamed = JourneyFile(
            "animaldoku",
            listOf(JourneyChapter(1, 100, 400, listOf(JourneyStop("a1", "easy", 150, 90), JourneyStop("a2", "easy", 150, 90, replaces = "old2")))),
        )
        val progress = mapOf("a1" to won(3, 60), "a2" to won(3, 60))
        val seconds = mapOf("a1" to 40, "a2" to 20, "old2" to 70)

        assertEquals(130, chapterCelebrationModel(journeyRows(renamed, progress), 1, seconds)!!.playSeconds)
        assertEquals(130, journeyCelebrationModel(journeyRows(renamed, progress), seconds)!!.playSeconds)
    }
}
