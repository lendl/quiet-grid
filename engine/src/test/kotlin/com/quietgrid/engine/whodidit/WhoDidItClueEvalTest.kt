package com.quietgrid.engine.whodidit

import com.quietgrid.engine.whodidit.WhoDidItClue.*
import com.quietgrid.engine.whodidit.WhoDidItTrait.*
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class WhoDidItClueEvalTest {
    private val spec = easyFixture()
    private val solution = fixtureSolution()
    private val s0 = WhoDidItRef(0, 0)
    private val place0 = WhoDidItRef(1, 0)
    private val place1 = WhoDidItRef(1, 1)
    private val item0 = WhoDidItRef(2, 0)
    private val item2 = WhoDidItRef(2, 2)

    @Test
    fun sameTrueAndFalse() {
        assertTrue(Same(s0, place1).isTrue(spec, solution))
        assertFalse(Same(s0, place0).isTrue(spec, solution))
    }

    @Test
    fun notSameTrueAndFalse() {
        assertTrue(NotSame(s0, place0).isTrue(spec, solution))
        assertFalse(NotSame(s0, place1).isTrue(spec, solution))
    }

    @Test
    fun eitherIsExclusive() {
        assertTrue(Either(s0, item2, item0).isTrue(spec, solution))
        assertFalse(Either(s0, item2, place1).isTrue(spec, solution))
        assertFalse(Either(s0, item0, place0).isTrue(spec, solution))
    }

    @Test
    fun hasTraitTrueAndFalse() {
        assertTrue(HasTrait(s0, PLACE_OUTDOOR).isTrue(spec, solution))
        assertFalse(HasTrait(s0, PLACE_INDOOR).isTrue(spec, solution))
        assertTrue(HasTrait(place1, SUSPECT_TALL).isTrue(spec, solution))
        assertFalse(HasTrait(item0, ITEM_METAL).isTrue(spec, solution))
    }

    @Test
    fun lacksTraitTrueAndFalse() {
        assertTrue(LacksTrait(s0, PLACE_INDOOR).isTrue(spec, solution))
        assertFalse(LacksTrait(s0, PLACE_OUTDOOR).isTrue(spec, solution))
    }

    @Test
    fun crimeTrueAndFalse() {
        assertTrue(Crime(WhoDidItRef(2, 1)).isTrue(spec, solution))
        assertFalse(Crime(item0).isTrue(spec, solution))
    }

    @Test
    fun crimeTraitTrueAndFalse() {
        assertTrue(CrimeTrait(ITEM_METAL).isTrue(spec, solution))
        assertTrue(CrimeTrait(SUSPECT_SCARF).isTrue(spec, solution))
        assertFalse(CrimeTrait(ITEM_WOOD).isTrue(spec, solution))
        assertFalse(CrimeTrait(PLACE_OUTDOOR).isTrue(spec, solution))
    }

    @Test
    fun negatedRoundTrips() {
        val clues = listOf<WhoDidItClue>(
            Same(s0, place1), NotSame(s0, place1), HasTrait(s0, PLACE_OUTDOOR), LacksTrait(s0, PLACE_OUTDOOR),
        )
        for (clue in clues) {
            assertEquals(clue, clue.negated().negated())
            assertEquals(!clue.isTrue(spec, solution), clue.negated().isTrue(spec, solution))
        }
    }

    @Test
    fun negatedRejectsOtherTypes() {
        assertThrows(IllegalArgumentException::class.java) { Crime(item0).negated() }
        assertThrows(IllegalArgumentException::class.java) { CrimeTrait(ITEM_WOOD).negated() }
        assertThrows(IllegalArgumentException::class.java) { Either(s0, item0, item2).negated() }
    }

    @Test
    fun consistencyHonoursLiarRule() {
        assertTrue(isConsistent(liarFixture(), solution))
        assertFalse(isConsistent(liarFixture(), solution.copy(culprit = 0)))
        assertTrue(isConsistent(easyFixture(), solution))
        assertFalse(isConsistent(easyFixture(), solution.copy(culprit = 1)))
    }
}
