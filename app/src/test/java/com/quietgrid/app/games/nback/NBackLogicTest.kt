package com.quietgrid.app.games.nback

import com.quietgrid.app.core.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NBackLogicTest {

    @Test
    fun `generateNBackTrials produces exactly totalTrials positions in range`() {
        val trials = generateNBackTrials(n = 2, random = Random(1))
        assertEquals(NBACK_TOTAL_TRIALS, trials.size)
        assertTrue(trials.all { it.position in 0 until NBACK_GRID_SIZE })
    }

    @Test
    fun `the first n trials are never targets`() {
        for (n in 1..4) {
            val trials = generateNBackTrials(n = n, random = Random(n.toLong()))
            assertTrue(trials.take(n).none { it.isTargetMatch })
        }
    }

    @Test
    fun `target rate lands near 28 percent of eligible trials with a fixed seed`() {
        val trials = generateNBackTrials(n = 2, random = Random(42))
        val eligible = NBACK_TOTAL_TRIALS - 2
        val expectedTargets = (eligible * 0.28).let { Math.round(it) }.toInt()
        assertEquals(expectedTargets, trials.count { it.isTargetMatch })
    }

    @Test
    fun `isTargetMatch is consistent with the actual n-back position`() {
        val trials = generateNBackTrials(n = 3, random = Random(7))
        trials.forEachIndexed { index, trial ->
            if (index >= 3) {
                assertEquals(trial.position == trials[index - 3].position, trial.isTargetMatch)
            } else {
                assertFalse(trial.isTargetMatch)
            }
        }
    }

    @Test
    fun `createNBackSession sizes n and interval per difficulty`() {
        assertEquals(1, createNBackSession(Difficulty.EASY).config.n)
        assertEquals(2500L, createNBackSession(Difficulty.EASY).config.intervalMs)
        assertEquals(2, createNBackSession(Difficulty.MEDIUM).config.n)
        assertEquals(2000L, createNBackSession(Difficulty.MEDIUM).config.intervalMs)
        assertEquals(3, createNBackSession(Difficulty.HARD).config.n)
        assertEquals(1500L, createNBackSession(Difficulty.HARD).config.intervalMs)
        assertEquals(4, createNBackSession(Difficulty.EXPERT).config.n)
        assertEquals(1250L, createNBackSession(Difficulty.EXPERT).config.intervalMs)
        assertEquals(NBACK_TOTAL_TRIALS, createNBackSession(Difficulty.EASY).trials.size)
    }

    @Test
    fun `computeNBackScore rewards accuracy and penalizes false positives`() {
        val perfectHitsOnly = listOf(
            NBackTrial(position = 0, isTargetMatch = true, responded = true, reactionTimeMs = 500L),
            NBackTrial(position = 1, isTargetMatch = true, responded = true, reactionTimeMs = 500L),
            NBackTrial(position = 2, isTargetMatch = false, responded = false),
        )
        assertEquals(10000 + 800, computeNBackScore(perfectHitsOnly))

        val noHitsOneFalsePositive = listOf(
            NBackTrial(position = 0, isTargetMatch = true, responded = false),
            NBackTrial(position = 1, isTargetMatch = true, responded = false),
            NBackTrial(position = 2, isTargetMatch = false, responded = true),
        )
        assertEquals(0, computeNBackScore(noHitsOneFalsePositive))
    }

    @Test
    fun `computeNBackScore is zero when there are no target trials`() {
        val noTargets = listOf(NBackTrial(position = 0, isTargetMatch = false, responded = false))
        assertEquals(0, computeNBackScore(noTargets))
    }

    @Test
    fun `nbackAccuracyPct counts both hits and correct rejections as correct`() {
        val trials = listOf(
            NBackTrial(position = 0, isTargetMatch = true, responded = true),
            NBackTrial(position = 1, isTargetMatch = false, responded = false),
            NBackTrial(position = 2, isTargetMatch = true, responded = false),
            NBackTrial(position = 3, isTargetMatch = false, responded = true),
        )
        assertEquals(50, nbackAccuracyPct(trials))
    }

    @Test
    fun `hits misses and false positives count the four outcome cells`() {
        val trials = listOf(
            NBackTrial(position = 0, isTargetMatch = true, responded = true),
            NBackTrial(position = 1, isTargetMatch = true, responded = false),
            NBackTrial(position = 2, isTargetMatch = false, responded = true),
            NBackTrial(position = 3, isTargetMatch = false, responded = false),
        )
        assertEquals(1, nbackHits(trials))
        assertEquals(1, nbackMisses(trials))
        assertEquals(1, nbackFalsePositives(trials))
    }

    @Test
    fun `nbackAverageReactionTimeMs averages only responded hit trials`() {
        val trials = listOf(
            NBackTrial(position = 0, isTargetMatch = true, responded = true, reactionTimeMs = 400L),
            NBackTrial(position = 1, isTargetMatch = true, responded = true, reactionTimeMs = 600L),
            NBackTrial(position = 2, isTargetMatch = false, responded = true, reactionTimeMs = 100L),
        )
        assertEquals(500, nbackAverageReactionTimeMs(trials))
    }

    @Test
    fun `nbackHasMeaningfulProgress is always false`() {
        assertFalse(nbackHasMeaningfulProgress(createNBackSession(Difficulty.EASY)))
    }
}
