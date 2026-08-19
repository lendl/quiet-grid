// engine/src/test/kotlin/com/quietgrid/engine/animaldoku/AnimalDokuDifficultyTest.kt
package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalDokuDifficultyTest {
    @Test
    fun `classifyAnimalDokuDifficulty returns null for an unsolved result`() {
        val result = AnimalDokuSolveResult(solved = false, steps = emptyList())
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `analyzeSolveResult returns hardest technique its repeats max chain depth and opening technique`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 4),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 7),
            ),
        )
        val profile = analyzeSolveResult(result)
        assertEquals(AnimalDokuTechnique.CHAIN, profile.hardestTechnique)
        assertEquals(2, profile.hardestTechniqueRepeats)
        assertEquals(7, profile.maxChainDepth)
        assertEquals(AnimalDokuTechnique.SINGLETON, profile.openingTechnique)
    }

    @Test
    fun `singleton-only within easy size range classifies as easy`() {
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0)),
        )
        assertEquals(Difficulty.EASY, classifyAnimalDokuDifficulty(5, result))
    }

    @Test
    fun `confinement-hardest puzzle at an easy size classifies as medium not easy`() {
        // EASY is SINGLETON-only (min == max == SINGLETON) -- a purely rule-teaching tier with no
        // real deduction required beyond the guaranteed freebie region. A CONFINEMENT-hardest
        // puzzle at size 4 (an EASY-range size) doesn't fit EASY's narrowed ceiling, so it falls
        // through to MEDIUM, whose size range was widened down to 4..6 specifically to absorb this
        // vacated territory rather than leaving size-4 CONFINEMENT puzzles unclassifiable.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
            ),
        )
        assertEquals(Difficulty.MEDIUM, classifyAnimalDokuDifficulty(4, result))
    }

    @Test
    fun `confinement at size outside every tiers range does not classify as easy`() {
        // EXPERT's size ceiling is 8, so size 9 falls outside every tier's size range at all (EASY
        // 4..5, MEDIUM 4..6, HARD 6..8, EXPERT 7..8) -- classification returns null purely on size,
        // before technique is even consulted.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0)),
        )
        assertNull(classifyAnimalDokuDifficulty(9, result))
    }

    @Test
    fun `pairing K equals 2 repeated four times opening with confinement classifies as hard`() {
        // HARD's technique ceiling is PAIRING_3, so a PAIRING_2-hardest puzzle fits its technique
        // range. HARD now requires the hardest technique to repeat at least FOUR times (raised from
        // 2, which real generation showed was satisfied by mere chance far too easily) and the
        // opening step to be at or below PAIRING_2 -- this trace opens with CONFINEMENT and fires
        // PAIRING_2 four times to legitimately satisfy both gates at the new bar.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
            ),
        )
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `pairing repeated only three times falls short of hards raised repeat floor`() {
        // Pins the exact new boundary: HARD's minHardestTechniqueRepeats is 4, so 3 occurrences --
        // one short -- must fail, distinct from the "fires only once" case which doesn't test how
        // close to the new bar a puzzle can get before still failing.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `chain depth 3 repeated twice within expert size range classifies as expert`() {
        // HARD's chain range is the empty 1..0, so a chain-hardest puzzle can never land in HARD
        // regardless of depth -- it either fits EXPERT's 3..10 range or classifies as null. Depth 3
        // is EXPERT's new floor (raised from the old "shallow" cap of 2, which is why this used to
        // be called "shallow chain" -- depth 3 is no longer shallow, it's the genuine floor). The
        // trace opens with SINGLETON (well under EXPERT's PAIRING_3 opening cap) and fires CHAIN
        // twice to satisfy EXPERT's minHardestTechniqueRepeats = 2.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain depth 2 no longer reaches experts raised depth floor`() {
        // Pins the exact new floor: depth 2 used to be EXPERT's entire allowed range (1..2); now
        // it's one below the new floor of 3, so an otherwise-qualifying trace (valid opening, fires
        // twice) must still fail purely on depth.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 2),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 2),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `a lone chain step opening the puzzle fails on both opening cap and repeat count`() {
        // A single CHAIN step at depth 3 (within EXPERT's new 3..10 floor) still fails: it OPENS the
        // puzzle with CHAIN itself (ordinal 5), blowing past every tier's maxOpeningTechnique cap
        // (EXPERT's is PAIRING_3, ordinal 3), and it only fires once, short of EXPERT's
        // minHardestTechniqueRepeats = 2. Either failure alone would be enough; both apply here.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3)),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `size outside every tiers range returns null`() {
        val result = AnimalDokuSolveResult(solved = true, steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0)))
        assertNull(classifyAnimalDokuDifficulty(20, result))
    }

    @Test
    fun `chain outside expert size range does not classify as expert`() {
        // Size 6 is compatible with MEDIUM (4..6) and HARD (6..8), but a lone CHAIN step opens the
        // puzzle with CHAIN itself, which exceeds both tiers' maxOpeningTechnique cap -- and EXPERT
        // (the only tier whose chain range is non-empty) doesn't cover size 6 at all (its range is
        // 7..8). No tier accepts it, so classification returns null.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3)),
        )
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `opening step harder than a tiers maxOpeningTechnique excludes that tier`() {
        // Hardest technique overall is PAIRING_3, which sits at HARD's technique ceiling, but the
        // puzzle OPENS with PAIRING_3 -- harder than HARD's maxOpeningTechnique (PAIRING_2) -- so
        // HARD rejects it on the opening cap alone (it would also fail HARD's raised repeat floor of
        // 4, firing only twice here, but the opening cap alone is sufficient to reject it). EXPERT
        // (also size-compatible at size 7) rejects it too, on its own existing floor: PAIRING_3 never
        // reaches EXPERT's minTechnique (PAIRING_4_PLUS). No tier accepts it.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(7, result))
    }

    @Test
    fun `hardest technique firing only once fails a tiers minHardestTechniqueRepeats`() {
        // At size 6, MEDIUM (4..6) is tried first: PAIRING_2 exceeds MEDIUM's technique ceiling
        // (CONFINEMENT) outright, so MEDIUM rejects it the ordinary way. HARD (6..8) is tried next:
        // the opening step (CONFINEMENT) is within HARD's opening cap and PAIRING_2 is within HARD's
        // technique range, but PAIRING_2 only fires once here -- far short of HARD's raised
        // minHardestTechniqueRepeats = 4 -- so HARD rejects it too. No tier accepts it.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(6, result))
    }

    @Test
    fun `both new checks passing alongside the existing ceiling check classifies as hard`() {
        // Opens with PAIRING_2 (at HARD's maxOpeningTechnique cap, inclusive boundary), hardest
        // technique is PAIRING_3 (at HARD's technique ceiling, inclusive boundary), and PAIRING_3
        // fires four times (meeting HARD's raised minHardestTechniqueRepeats = 4 exactly) -- every
        // gate passes at its boundary, so this should classify as HARD.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
            ),
        )
        assertEquals(Difficulty.HARD, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain hardest at depth 3 opening at the PAIRING_3 cap boundary classifies as expert`() {
        // Opens with PAIRING_3 -- exactly at EXPERT's maxOpeningTechnique boundary (inclusive) --
        // and CHAIN at depth 3 (EXPERT's new floor) fires twice, meeting EXPERT's raised
        // minHardestTechniqueRepeats = 2. HARD (also size-compatible at size 8) rejects it
        // regardless, since HARD's chain range is the empty 1..0. EXPERT is the only tier reachable
        // by a chain-hardest puzzle.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_3, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `chain hardest firing only once no longer classifies as expert now that its repeat floor is back to 2`() {
        // EXPERT's minHardestTechniqueRepeats is back to 2 (was loosened to 1 earlier, then real
        // evidence -- a competitor puzzle needing 5 chain steps to feel genuinely hard -- showed 1
        // was far too permissive). A single CHAIN step at a qualifying depth (3) with a valid
        // opening (PAIRING_2) no longer classifies as EXPERT; it must recur.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.PAIRING_2, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
            ),
        )
        assertNull(classifyAnimalDokuDifficulty(8, result))
    }

    @Test
    fun `real competitor puzzle trace classifies as expert under the recalibrated profile`() {
        // The exact 21-step trace our own solver produced when run against a hand-analyzed
        // competitor puzzle (a genuinely hard Duckdoku-style board): 4 CONFINEMENT steps, then five
        // CHAIN steps at depths 6, 6, 6, 3, 3 interleaved with more CONFINEMENT, then singleton
        // mop-up. Under the OLD profile this classified as null (chain depth 6 exceeded the old
        // "shallow only" cap of 2). Tested at size 8 (the largest size this app's generator ever
        // targets) rather than the original board's 9x9, since the size ceiling is a separate,
        // already-settled generation-bottleneck decision unrelated to this trace's actual technique
        // depth -- what's being verified here is that genuinely deep reasoning like this now has
        // somewhere to land.
        val result = AnimalDokuSolveResult(
            solved = true,
            steps = listOf(
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 6),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, 3),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
                AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0),
            ),
        )
        assertEquals(Difficulty.EXPERT, classifyAnimalDokuDifficulty(8, result))
    }
}
